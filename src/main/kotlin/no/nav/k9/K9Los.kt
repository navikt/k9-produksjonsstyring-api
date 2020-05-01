package no.nav.k9

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.application.*
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.features.*
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
import no.nav.k9.domene.repository.BehandlingProsessEventRepository
import no.nav.k9.domene.repository.OppgaveKøRepository
import no.nav.k9.domene.repository.OppgaveRepository
import no.nav.k9.integrasjon.abac.Decision
import no.nav.k9.integrasjon.abac.PepClient
import no.nav.k9.integrasjon.pdl.PdlService
import no.nav.k9.integrasjon.rest.RequestContextService
import no.nav.k9.integrasjon.sakogbehandling.SakOgBehadlingProducer
import no.nav.k9.kafka.AsynkronProsesseringV1Service
import no.nav.k9.tjenester.admin.AdminApis
import no.nav.k9.tjenester.avdelingsleder.AvdelingslederApis
import no.nav.k9.tjenester.avdelingsleder.AvdelingslederTjeneste
import no.nav.k9.tjenester.avdelingsleder.nøkkeltall.NøkkeltallApis
import no.nav.k9.tjenester.avdelingsleder.oppgaveko.AvdelingslederOppgavekøApis
import no.nav.k9.tjenester.avdelingsleder.saksbehandler.AvdelingslederSaksbehandlerApis
import no.nav.k9.tjenester.fagsak.FagsakApis
import no.nav.k9.tjenester.innsikt.InnsiktGrensesnitt
import no.nav.k9.tjenester.kodeverk.HentKodeverkTjeneste
import no.nav.k9.tjenester.kodeverk.KodeverkApis
import no.nav.k9.tjenester.konfig.KonfigApis
import no.nav.k9.tjenester.mock.MockGrensesnitt
import no.nav.k9.tjenester.saksbehandler.NavAnsattApis
import no.nav.k9.tjenester.saksbehandler.TestApis
import no.nav.k9.tjenester.saksbehandler.nøkkeltall.SaksbehandlerNøkkeltallApis
import no.nav.k9.tjenester.saksbehandler.oppgave.OppgaveApis
import no.nav.k9.tjenester.saksbehandler.oppgave.OppgaveTjeneste
import no.nav.k9.tjenester.saksbehandler.saksliste.SaksbehandlerSakslisteApis
import java.time.Duration

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

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

    install(StatusPages) {
        DefaultStatusPages()
        JacksonStatusPages()
        AuthStatusPages()
    }

    val accessTokenClientResolver = AccessTokenClientResolver(
        clients = configuration.clients()
    )

//    val gosysOppgaveGateway =
//        GosysOppgaveGateway(
//            httpClient = HttpClients.createSystem(),
//            uri = configuration.getOppgaveBaseUri(),
//            accessTokenClientResolver = accessTokenClientResolver
//        )

    val pdlService = PdlService(
        configuration.pdlUrl(),
        accessTokenClient = accessTokenClientResolver.naisSts(),
        configuration = configuration
    )
    val dataSource = hikariConfig(configuration)
    val oppgaveRepository = OppgaveRepository(dataSource)
    val oppgaveKøRepository = OppgaveKøRepository(dataSource)
    val oppgaveTjeneste = OppgaveTjeneste(
        oppgaveRepository,
        oppgaveKøRepository = oppgaveKøRepository,
        pdlService = pdlService
    )
    val avdelingslederTjeneste = AvdelingslederTjeneste(
        oppgaveKøRepository,
        oppgaveTjeneste
    )
    val behandlingProsessEventRepository = BehandlingProsessEventRepository(dataSource)

    val sakOgBehadlingProducer = SakOgBehadlingProducer(
        kafkaConfig = configuration.getKafkaConfig(),
        config = configuration
    )
    val k9sakEventHandler = K9sakEventHandler(
        oppgaveRepository = oppgaveRepository,
        behandlingProsessEventRepository = behandlingProsessEventRepository
        , config = configuration,
        sakOgBehadlingProducer = sakOgBehadlingProducer
    )

    val asynkronProsesseringV1Service = AsynkronProsesseringV1Service(
        kafkaConfig = configuration.getKafkaConfig(),
        configuration = configuration,
        k9sakEventHandler = k9sakEventHandler
    )

    environment.monitor.subscribe(ApplicationStopping) {
        log.info("Stopper AsynkronProsesseringV1Service.")
        asynkronProsesseringV1Service.stop()
        sakOgBehadlingProducer.stop()
        log.info("AsynkronProsesseringV1Service Stoppet.")
    }
    val requestContextService = RequestContextService()

    val pepClient = PepClient(configuration, Decision.Deny)
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
            InnsiktGrensesnitt(oppgaveRepository)
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
                    pepClient = pepClient
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
                pepClient = pepClient
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

    install(CallLogging) {
        correlationIdAndRequestIdInMdc()
        logRequests()
        mdc("id_token_jti") { call ->
            try {
                idTokenProvider.getIdToken(call).getId()
            } catch (cause: Throwable) {
                null
            }
        }
    }
}

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
    pepClient: PepClient
) {
    route("api") {
        AdminApis()
        route("fagsak"){
          FagsakApis(
              oppgaveTjeneste = oppgaveTjeneste,
              requestContextService = requestContextService,
              configuration = configuration
          )
        }
        AvdelingslederSaksbehandlerApis()
        NøkkeltallApis()
        route("saksbehandler") {
            route("oppgaver") {
                OppgaveApis(
                    configuration,
                    requestContextService,
                    oppgaveTjeneste,
                    pdlService = pdlService
                )
            }
            SaksbehandlerSakslisteApis(oppgaveTjeneste)
            SaksbehandlerNøkkeltallApis()
        }
        route("avdelingsleder") {
            AvdelingslederApis(
                avdelingslederTjeneste = avdelingslederTjeneste,
                oppgaveTjeneste = oppgaveTjeneste
            )
            route("oppgavekoer") {
                AvdelingslederOppgavekøApis(
                    avdelingslederTjeneste
                )
            }
        }
        NavAnsattApis(requestContextService, configuration)
        if (!configuration.erIProd) {
            TestApis(requestContextService, pdlService, accessTokenClientResolver, configuration, pepClient = pepClient)
        }
        SaksbehandlerNøkkeltallApis()
        route("konfig") { KonfigApis() }
        KodeverkApis(kodeverkTjeneste = kodeverkTjeneste)
    }
}
