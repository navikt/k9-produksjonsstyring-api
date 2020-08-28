package no.nav.k9

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.jackson.*
import io.ktor.locations.*
import io.ktor.metrics.micrometer.*
import io.ktor.routing.*
import io.ktor.util.*
import io.prometheus.client.hotspot.DefaultExports
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.broadcast
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import no.nav.helse.dusseldorf.ktor.auth.AuthStatusPages
import no.nav.helse.dusseldorf.ktor.auth.allIssuers
import no.nav.helse.dusseldorf.ktor.auth.multipleJwtIssuers
import no.nav.helse.dusseldorf.ktor.core.*
import no.nav.helse.dusseldorf.ktor.health.HealthReporter
import no.nav.helse.dusseldorf.ktor.jackson.JacksonStatusPages
import no.nav.helse.dusseldorf.ktor.jackson.dusseldorfConfigured
import no.nav.helse.dusseldorf.ktor.metrics.MetricsRoute
import no.nav.helse.dusseldorf.ktor.metrics.init
import no.nav.k9.domene.lager.oppgave.Oppgave
import no.nav.k9.domene.modell.BehandlingStatus
import no.nav.k9.domene.modell.FagsakYtelseType
import no.nav.k9.domene.repository.*
import no.nav.k9.eventhandler.køOppdatertProsessor
import no.nav.k9.eventhandler.oppdatereKøerMedOppgaveProsessor
import no.nav.k9.integrasjon.datavarehus.StatistikkProducer
import no.nav.k9.integrasjon.kafka.AsynkronProsesseringV1Service
import no.nav.k9.integrasjon.sakogbehandling.SakOgBehadlingProducer
import no.nav.k9.tjenester.admin.AdminApis
import no.nav.k9.tjenester.avdelingsleder.AvdelingslederApis
import no.nav.k9.tjenester.avdelingsleder.nokkeltall.AlleOppgaverNyeOgFerdigstilte
import no.nav.k9.tjenester.avdelingsleder.nokkeltall.NokkeltallApis
import no.nav.k9.tjenester.avdelingsleder.oppgaveko.AvdelingslederOppgavekøApis
import no.nav.k9.tjenester.driftsmeldinger.DriftsmeldingerApis
import no.nav.k9.tjenester.fagsak.FagsakApis
import no.nav.k9.tjenester.innsikt.innsiktGrensesnitt
import no.nav.k9.tjenester.kodeverk.KodeverkApis
import no.nav.k9.tjenester.konfig.KonfigApis
import no.nav.k9.tjenester.mock.MockGrensesnitt
import no.nav.k9.tjenester.saksbehandler.NavAnsattApis
import no.nav.k9.tjenester.saksbehandler.nokkeltall.SaksbehandlerNøkkeltallApis
import no.nav.k9.tjenester.saksbehandler.oppgave.OppgaveApis
import no.nav.k9.tjenester.saksbehandler.saksliste.SaksbehandlerOppgavekoApis
import no.nav.k9.tjenester.sse.Sse
import no.nav.k9.tjenester.sse.SseEvent
import org.koin.core.qualifier.named
import org.koin.ktor.ext.Koin
import org.koin.ktor.ext.getKoin
import java.time.Duration
import java.util.*
import java.util.concurrent.Executors
import kotlin.system.measureTimeMillis

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@ExperimentalCoroutinesApi
@KtorExperimentalAPI
@KtorExperimentalLocationsAPI
fun Application.k9Los() {
    val appId = environment.config.id()
    logProxyProperties()
    DefaultExports.initialize()

    val configuration = Configuration(environment.config)
    val issuers = configuration.issuers()

    install(Koin) {
        modules(selectModuleBasedOnProfile(this@k9Los, config = configuration))
    }
    val koin = getKoin()
    install(Authentication) {
        multipleJwtIssuers(issuers)
    }

    install(ContentNegotiation) {
        jackson {
            dusseldorfConfigured()
                .setPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CAMEL_CASE)
                .configure(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false)
        }
    }

    install(StatusPages) {
        DefaultStatusPages()
        JacksonStatusPages()
        AuthStatusPages()
    }

    val køOppdatertProsessorJob =
        køOppdatertProsessor(
            oppgaveKøRepository = koin.get(),
            oppgaveRepository = koin.get(),
            channel = koin.get<Channel<UUID>>(named("oppgaveKøOppdatert")),
            reservasjonRepository = koin.get()
        )

    val oppdatereKøerMedOppgaveProsessorJob =
        oppdatereKøerMedOppgaveProsessor(
            oppgaveKøRepository = koin.get(),
            channel = koin.get<Channel<Oppgave>>(named("oppgaveChannel")),
            reservasjonRepository = koin.get(),
            pepClient = koin.get()
        )

    val asynkronProsesseringV1Service = koin.get<AsynkronProsesseringV1Service>()
    val sakOgBehadlingProducer = koin.get<SakOgBehadlingProducer>()
    val statistikkProducer = koin.get<StatistikkProducer>()
    
    environment.monitor.subscribe(ApplicationStopping) {
        log.info("Stopper AsynkronProsesseringV1Service.")
        asynkronProsesseringV1Service.stop()
        sakOgBehadlingProducer.stop()
        statistikkProducer.stop()
        log.info("AsynkronProsesseringV1Service Stoppet.")
        log.info("Stopper pipeline")
        køOppdatertProsessorJob.cancel()
        oppdatereKøerMedOppgaveProsessorJob.cancel()
    }

    // Server side events
    val sseChannel = produce {
        for (oppgaverOppdatertEvent in koin.get<Channel<SseEvent>>(named("refreshKlienter"))) {
            send(oppgaverOppdatertEvent)
        }
    }.broadcast()

    // Synkroniser oppgaver
    // regenererOppgaver(oppgaveRepository, behandlingProsessEventRepository, reservasjonRepository, oppgaveKøRepository)


    // rekjørForGrafer(koin.get(), koin.get())

    install(CallIdRequired)

    install(Locations)

    install(Routing) {

        MetricsRoute()
        DefaultProbeRoutes()

        HealthReporter(
            app = appId,
            healthService = koin.get(),
            frequency = Duration.ofMinutes(1)
        )

        route("mock") {
            MockGrensesnitt()
        }

        route("innsikt") {
            innsiktGrensesnitt()
        }

        if ((KoinProfile.LOCAL == koin.get<KoinProfile>())) {
            api(sseChannel)
        } else {
            authenticate(*issuers.allIssuers()) {
                api(sseChannel)
            }
        }

        static("static") {
            resources("static/css")
            resources("static/js")
        }
    }

    install(MicrometerMetrics) {
        init(appId)
    }

    intercept(ApplicationCallPipeline.Monitoring) {
        call.request.log()
    }

    install(CallId) {
        generated()
    }
}

@ExperimentalCoroutinesApi
@KtorExperimentalAPI
@KtorExperimentalLocationsAPI
private fun Route.api(sseChannel: BroadcastChannel<SseEvent>) {
    install(CORS) {
        method(HttpMethod.Options)
        anyHost()
        allowCredentials = true
    }
    route("api") {

        AdminApis()
        route("driftsmeldinger") {
            DriftsmeldingerApis()
        }
        route("fagsak") {
            FagsakApis()
        }
        route("saksbehandler") {
            route("oppgaver") {
                OppgaveApis()
            }

            SaksbehandlerOppgavekoApis()
            SaksbehandlerNøkkeltallApis()
        }
        route("avdelingsleder") {
            AvdelingslederApis()
            route("oppgavekoer") {
                AvdelingslederOppgavekøApis()
            }
            route("nokkeltall") {
                NokkeltallApis()
            }
        }

        NavAnsattApis()

        route("konfig") { KonfigApis() }
        KodeverkApis()
        Sse(
            sseChannel = sseChannel
        )
    }
}

private fun Application.rekjørForGrafer(
    behandlingProsessEventRepository: BehandlingProsessEventRepository,
    statistikkRepository: StatistikkRepository
) {
    launch(Executors.newSingleThreadExecutor().asCoroutineDispatcher()) {
        val alleEventerIder = behandlingProsessEventRepository.hentAlleEventerIder()
        for ((index, eventId) in alleEventerIder.withIndex()) {
            if (index % 1000 == 0) {
                log.info("""Ferdig med $index av ${alleEventerIder.size}""")
            }
            for (modell in behandlingProsessEventRepository.hent(UUID.fromString(eventId)).alleVersjoner()) {
                val oppgave = modell.oppgave()
                if (modell.starterSak()) {
                    if (oppgave.aktiv && oppgave.fagsakYtelseType != FagsakYtelseType.FRISINN) {
                        statistikkRepository.lagre(
                            AlleOppgaverNyeOgFerdigstilte(
                                oppgave
                                    .fagsakYtelseType, oppgave.behandlingType, oppgave.eventTid.toLocalDate()
                            )
                        ) {
                            it.nye.add(oppgave.eksternId.toString())
                            it
                        }
                    }
                }
                if (oppgave.behandlingStatus == BehandlingStatus.AVSLUTTET && oppgave.fagsakYtelseType != FagsakYtelseType.FRISINN) {
                    statistikkRepository.lagre(
                        AlleOppgaverNyeOgFerdigstilte(
                            oppgave
                                .fagsakYtelseType, oppgave.behandlingType, oppgave.eventTid.toLocalDate()
                        )
                    ) {
                        it.ferdigstilte.add(oppgave.eksternId.toString())
                        it
                    }
                }
            }
        }
    }
}


private fun Application.regenererOppgaver(
    oppgaveRepository: OppgaveRepository,
    behandlingProsessEventRepository: BehandlingProsessEventRepository,
    reservasjonRepository: ReservasjonRepository,
    oppgaveKøRepository: OppgaveKøRepository,
    saksbehhandlerRepository: SaksbehandlerRepository
) {
    launch(Executors.newSingleThreadExecutor().asCoroutineDispatcher()) {
        log.info("Starter oppgavesynkronisering")
        val measureTimeMillis = measureTimeMillis {

            for (aktivOppgave in oppgaveRepository.hentAktiveOppgaver()) {
                val event = behandlingProsessEventRepository.hent(aktivOppgave.eksternId)
                val oppgave = event.oppgave()
                if (!oppgave.aktiv) {
                    if (reservasjonRepository.finnes(oppgave.eksternId)) {
                        reservasjonRepository.lagre(oppgave.eksternId) { reservasjon ->
                            reservasjon!!.reservertTil = null
                            runBlocking {
                                saksbehhandlerRepository.fjernReservasjon(
                                    reservasjon.reservertAv,
                                    reservasjon.oppgave
                                )
                            }
                            reservasjon
                        }
                    }
                }
                oppgaveRepository.lagre(oppgave.eksternId) {
                    oppgave
                }
            }
            val oppgaver = oppgaveRepository.hentAktiveOppgaver()
            for (oppgavekø in oppgaveKøRepository.hent()) {
                for (oppgave in oppgaver) {
                    if (oppgavekø.leggOppgaveTilEllerFjernFraKø(oppgave, reservasjonRepository)) {
                        oppgaveKøRepository.lagre(oppgavekø.id) { forrige ->
                            forrige?.leggOppgaveTilEllerFjernFraKø(oppgave, reservasjonRepository)
                            forrige!!
                        }
                    }
                }
                oppgaveKøRepository.lagre(oppgavekø.id) { forrige ->
                    forrige!!
                }
            }
        }
        log.info("Avslutter oppgavesynkronisering: $measureTimeMillis ms")

    }
}


