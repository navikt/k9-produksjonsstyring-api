package no.nav.k9

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.application.*
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.features.CORS
import io.ktor.features.CallId
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.http.HttpMethod
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.jackson.jackson
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Locations
import io.ktor.metrics.micrometer.MicrometerMetrics
import io.ktor.routing.Route
import io.ktor.routing.Routing
import io.ktor.routing.route
import io.ktor.util.KtorExperimentalAPI
import io.prometheus.client.hotspot.DefaultExports
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.broadcast
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.launch
import no.nav.helse.dusseldorf.ktor.auth.AuthStatusPages
import no.nav.helse.dusseldorf.ktor.auth.allIssuers
import no.nav.helse.dusseldorf.ktor.auth.multipleJwtIssuers
import no.nav.helse.dusseldorf.ktor.core.*
import no.nav.helse.dusseldorf.ktor.health.HealthReporter
import no.nav.helse.dusseldorf.ktor.health.HealthService
import no.nav.helse.dusseldorf.ktor.jackson.JacksonStatusPages
import no.nav.helse.dusseldorf.ktor.jackson.dusseldorfConfigured
import no.nav.helse.dusseldorf.ktor.metrics.MetricsRoute
import no.nav.helse.dusseldorf.ktor.metrics.init
import no.nav.k9.aksjonspunktbehandling.K9sakEventHandler
import no.nav.k9.auth.IdTokenProvider
import no.nav.k9.db.hikariConfig
import no.nav.k9.domene.lager.oppgave.Oppgave
import no.nav.k9.domene.repository.*
import no.nav.k9.eventhandler.køOppdatertProsessor
import no.nav.k9.eventhandler.oppdatereKøerMedOppgaveProsessor
import no.nav.k9.integrasjon.abac.PepClient
import no.nav.k9.integrasjon.audit.Auditlogger
import no.nav.k9.integrasjon.azuregraph.AzureGraphService
import no.nav.k9.integrasjon.datavarehus.StatistikkProducer
import no.nav.k9.integrasjon.kafka.AsynkronProsesseringV1Service
import no.nav.k9.integrasjon.pdl.PdlService
import no.nav.k9.integrasjon.rest.RequestContextService
import no.nav.k9.integrasjon.sakogbehandling.SakOgBehadlingProducer
import no.nav.k9.tjenester.admin.AdminApis
import no.nav.k9.tjenester.avdelingsleder.AvdelingslederApis
import no.nav.k9.tjenester.avdelingsleder.AvdelingslederTjeneste
import no.nav.k9.tjenester.avdelingsleder.nokkeltall.NokkeltallApis
import no.nav.k9.tjenester.avdelingsleder.nokkeltall.NokkeltallTjeneste
import no.nav.k9.tjenester.avdelingsleder.oppgaveko.AvdelingslederOppgavekøApis
import no.nav.k9.tjenester.fagsak.FagsakApis
import no.nav.k9.tjenester.innsikt.InnsiktGrensesnitt
import no.nav.k9.tjenester.kodeverk.HentKodeverkTjeneste
import no.nav.k9.tjenester.kodeverk.KodeverkApis
import no.nav.k9.tjenester.konfig.KonfigApis
import no.nav.k9.tjenester.mock.MockGrensesnitt
import no.nav.k9.tjenester.saksbehandler.NavAnsattApis
import no.nav.k9.tjenester.saksbehandler.TestApis
import no.nav.k9.tjenester.saksbehandler.nokkeltall.SaksbehandlerNøkkeltallApis
import no.nav.k9.tjenester.saksbehandler.oppgave.OppgaveApis
import no.nav.k9.tjenester.saksbehandler.oppgave.OppgaveTjeneste
import no.nav.k9.tjenester.saksbehandler.saksliste.SaksbehandlerOppgavekoApis
import no.nav.k9.tjenester.sse.Sse
import no.nav.k9.tjenester.sse.SseEvent
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
    val idTokenProvider = IdTokenProvider(cookieName = configuration.getCookieName())

//    install(CallLogging) {
//        correlationIdAndRequestIdInMdc()
//        logRequests()
//        mdc("id_token_jti") { call ->
//            try {
//                idTokenProvider.getIdToken(call).getId()
//            } catch (cause: Throwable) {
//                null
//            }
//        }
//    }

    install(StatusPages) {
        DefaultStatusPages()
        JacksonStatusPages()
        AuthStatusPages()
    }

    val accessTokenClientResolver = AccessTokenClientResolver(
        clients = configuration.clients()
    )

    val pdlService = PdlService(
        configuration.pdlUrl(),
        accessTokenClient = accessTokenClientResolver.naisSts(),
        configuration = configuration
    )
    val auditlogger = Auditlogger(configuration)
    val oppgaveKøOppdatert = Channel<UUID>(Channel.UNLIMITED)
    val oppgaverSomSkalInnPåKøer = Channel<Oppgave>(Channel.UNLIMITED)
    val refreshKlienter = Channel<SseEvent>()

    val dataSource = hikariConfig(configuration)
    val oppgaveRepository = OppgaveRepository(dataSource)

    val oppgaveKøRepository = OppgaveKøRepository(
        dataSource = dataSource,
        oppgaveKøOppdatert = oppgaveKøOppdatert,
        refreshKlienter = refreshKlienter
    )

    val saksbehandlerRepository = SaksbehandlerRepository(dataSource)
    val reservasjonRepository = ReservasjonRepository(
        oppgaveRepository = oppgaveRepository,
        oppgaveKøRepository = oppgaveKøRepository,
        dataSource = dataSource,
        refreshKlienter = refreshKlienter,
        saksbehandlerRepository = saksbehandlerRepository
    )
    val køOppdatertProsessorJob =
        køOppdatertProsessor(
            oppgaveKøRepository = oppgaveKøRepository,
            oppgaveRepository = oppgaveRepository,
            channel = oppgaveKøOppdatert,
            reservasjonRepository = reservasjonRepository
        )
    val oppdatereKøerMedOppgaveProsessorJob =
        oppdatereKøerMedOppgaveProsessor(
            oppgaveKøRepository = oppgaveKøRepository,
            channel = oppgaverSomSkalInnPåKøer,
            reservasjonRepository = reservasjonRepository
        )

    val behandlingProsessEventRepository = BehandlingProsessEventRepository(dataSource)

    val sakOgBehadlingProducer = SakOgBehadlingProducer(
        kafkaConfig = configuration.getKafkaConfig(),
        config = configuration
    )
    val azureGraphService = AzureGraphService(
        accessTokenClient = accessTokenClientResolver.accessTokenClient(),
        configuration = configuration
    )
    val statistikkRepository = StatistikkRepository(dataSource)

    val nokkeltallTjeneste = NokkeltallTjeneste(oppgaveRepository, statistikkRepository)

    val pepClient = PepClient(azureGraphService = azureGraphService, auditlogger = auditlogger, config = configuration)

    val statistikkProducer = StatistikkProducer(
        kafkaConfig = configuration.getKafkaConfig(),
        config = configuration,
        pepClient = pepClient,
        saksbehandlerRepository = saksbehandlerRepository,
        reservasjonRepository = reservasjonRepository
    )

    val k9sakEventHandler = K9sakEventHandler(
        oppgaveRepository = oppgaveRepository,
        behandlingProsessEventRepository = behandlingProsessEventRepository,
        config = configuration,
        sakOgBehadlingProducer = sakOgBehadlingProducer,
        oppgaveKøRepository = oppgaveKøRepository,
        reservasjonRepository = reservasjonRepository,
        statistikkProducer = statistikkProducer,
        oppgaverSomSkalInnPåKøer = oppgaverSomSkalInnPåKøer,
        statistikkRepository = statistikkRepository
    )

    val asynkronProsesseringV1Service = AsynkronProsesseringV1Service(
        kafkaConfig = configuration.getKafkaConfig(),
        configuration = configuration,
        k9sakEventHandler = k9sakEventHandler
    )

    val oppgaveTjeneste = OppgaveTjeneste(
        oppgaveRepository = oppgaveRepository,
        oppgaveKøRepository = oppgaveKøRepository,
        saksbehandlerRepository = saksbehandlerRepository,
        reservasjonRepository = reservasjonRepository,
        pdlService = pdlService,
        configuration = configuration,
        pepClient = pepClient,
        azureGraphService = azureGraphService,
        statistikkRepository = statistikkRepository
    )


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
    val avdelingslederTjeneste = AvdelingslederTjeneste(
        oppgaveKøRepository,
        saksbehandlerRepository,
        oppgaveTjeneste,
        reservasjonRepository,
        oppgaveRepository
    )


    // Server side events
    val sseChannel = produce {
        for (oppgaverOppdatertEvent in refreshKlienter) {
            send(oppgaverOppdatertEvent)
        }
    }.broadcast()

    // Synkroniser oppgaver
    // regenererOppgaver(oppgaveRepository, behandlingProsessEventRepository, reservasjonRepository, oppgaveKøRepository)
    regenererOppgaver2(oppgaveRepository, behandlingProsessEventRepository)
    val requestContextService = RequestContextService()
    install(CallIdRequired)

    install(Locations)

    install(Routing) {

        val kodeverkTjeneste = HentKodeverkTjeneste()

        MetricsRoute()
        DefaultProbeRoutes()

        val healthService = HealthService(
            healthChecks = asynkronProsesseringV1Service.isReadyChecks()
        )

        HealthReporter(
            app = appId,
            healthService = healthService,
            frequency = Duration.ofMinutes(1)
        )
        if (!configuration.erIProd) {
            route("mock") {
                MockGrensesnitt(k9sakEventHandler, behandlingProsessEventRepository)
            }
        }
        route("innsikt") {
            InnsiktGrensesnitt(oppgaveRepository, behandlingProsessEventRepository)
        }
        if (configuration.erIkkeLokalt) {
            authenticate(*issuers.allIssuers()) {
                api(
                    requestContextService,
                    oppgaveTjeneste,
                    avdelingslederTjeneste = avdelingslederTjeneste,
                    kodeverkTjeneste = kodeverkTjeneste,
                    pdlService = pdlService,
                    accessTokenClientResolver = accessTokenClientResolver,
                    configuration = configuration,
                    pepClient = pepClient,
                    saksbehhandlerRepository = saksbehandlerRepository,
                    azureGraphService = azureGraphService,
                    oppgaveKøRepository = oppgaveKøRepository,
                    oppgaveRepository = oppgaveRepository,
                    reservasjonRepository = reservasjonRepository,
                    eventRepository = behandlingProsessEventRepository,
                    sseChannel = sseChannel,
                    nokkeltallTjeneste = nokkeltallTjeneste
                )
            }
        } else {
            install(CORS) {
                method(HttpMethod.Options)
                anyHost()
                allowCredentials = true
            }
            api(
                requestContextService,
                oppgaveTjeneste,
                kodeverkTjeneste,
                avdelingslederTjeneste,
                pdlService = pdlService,
                accessTokenClientResolver = accessTokenClientResolver,
                configuration = configuration,
                pepClient = pepClient,
                saksbehhandlerRepository = saksbehandlerRepository,
                azureGraphService = azureGraphService,
                oppgaveKøRepository = oppgaveKøRepository,
                oppgaveRepository = oppgaveRepository,
                reservasjonRepository = reservasjonRepository,
                eventRepository = behandlingProsessEventRepository,
                sseChannel = sseChannel,
                nokkeltallTjeneste = nokkeltallTjeneste
            )
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
                            saksbehhandlerRepository.fjernReservasjon(reservasjon.reservertAv, reservasjon.oppgave)
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


private fun Application.regenererOppgaver2(
    oppgaveRepository: OppgaveRepository,
    behandlingProsessEventRepository: BehandlingProsessEventRepository
) {
    launch(Executors.newSingleThreadExecutor().asCoroutineDispatcher()) {
        log.info("Starter oppgavemigrering")
        val measureTimeMillis = measureTimeMillis {

            val ider = behandlingProsessEventRepository.hentAlleEventerIder()
            var count = 0
            for (id in ider) {
                try {
                    oppgaveRepository.lagre(UUID.fromString(id)) {
                        behandlingProsessEventRepository.hent(UUID.fromString(id)).oppgave()
                    }
                } catch (e: Exception) {
                    log.error("", e)
                }
                count++
                if (count % 1000 == 0) {
                    log.info("""$count av ${ider.size} ferdig""")
                }
            }
        }
        log.info("Avslutter oppgavesynkronisering: $measureTimeMillis ms")

    }
}

@ExperimentalCoroutinesApi
@KtorExperimentalAPI
@KtorExperimentalLocationsAPI
private fun Route.api(
    requestContextService: RequestContextService,
    oppgaveTjeneste: OppgaveTjeneste,
    kodeverkTjeneste: HentKodeverkTjeneste,
    avdelingslederTjeneste: AvdelingslederTjeneste,
    pdlService: PdlService,
    accessTokenClientResolver: AccessTokenClientResolver,
    configuration: Configuration,
    pepClient: PepClient,
    saksbehhandlerRepository: SaksbehandlerRepository,
    azureGraphService: AzureGraphService,
    eventRepository: BehandlingProsessEventRepository,
    oppgaveKøRepository: OppgaveKøRepository,
    oppgaveRepository: OppgaveRepository,
    reservasjonRepository: ReservasjonRepository,
    sseChannel: BroadcastChannel<SseEvent>,
    nokkeltallTjeneste: NokkeltallTjeneste
) {

    route("api") {
        AdminApis(
            behandlingProsessEventRepository = eventRepository,
            oppgaveRepository = oppgaveRepository,
            reservasjonRepository = reservasjonRepository,
            oppgaveKøRepository = oppgaveKøRepository
        )
        route("fagsak") {
            FagsakApis(
                oppgaveTjeneste = oppgaveTjeneste,
                requestContextService = requestContextService,
                configuration = configuration
            )
        }
        route("saksbehandler") {
            route("oppgaver") {
                OppgaveApis(
                    configuration = configuration,
                    requestContextService = requestContextService,
                    oppgaveTjeneste = oppgaveTjeneste,
                    saksbehandlerRepository = saksbehhandlerRepository
                )
            }

            SaksbehandlerOppgavekoApis(
                configuration = configuration,
                oppgaveTjeneste = oppgaveTjeneste,
                pepClient = pepClient,
                requestContextService = requestContextService,
                oppgaveKøRepository = oppgaveKøRepository
            )
            SaksbehandlerNøkkeltallApis(oppgaveTjeneste = oppgaveTjeneste)
        }
        route("avdelingsleder") {
            AvdelingslederApis(
                oppgaveTjeneste = oppgaveTjeneste,
                avdelingslederTjeneste = avdelingslederTjeneste
            )
            route("oppgavekoer") {
                AvdelingslederOppgavekøApis(
                    avdelingslederTjeneste
                )
            }
            route("nokkeltall") {
                NokkeltallApis(nokkeltallTjeneste = nokkeltallTjeneste)
            }
        }

        NavAnsattApis(
            requestContextService = requestContextService,
            pepClient = pepClient,
            configuration = configuration,
            azureGraphService = azureGraphService,
            saksbehandlerRepository = saksbehhandlerRepository
        )


        if (!configuration.erIProd) {
            TestApis(
                requestContextService = requestContextService,
                pdlService = pdlService,
                accessTokenClientResolver = accessTokenClientResolver,
                configuration = configuration,
                accessTokenClient = accessTokenClientResolver.accessTokenClient(),
                pepClient = pepClient
            )
        }
        route("konfig") { KonfigApis(configuration) }
        KodeverkApis(kodeverkTjeneste = kodeverkTjeneste)
        Sse(sseChannel = sseChannel)
    }
}

