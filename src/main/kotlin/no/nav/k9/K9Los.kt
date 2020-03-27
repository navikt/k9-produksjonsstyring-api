package no.nav.k9

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.application.*
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.features.CallId
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.jackson.jackson
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Locations
import io.ktor.metrics.micrometer.MicrometerMetrics
import io.ktor.routing.Routing
import io.ktor.routing.route
import io.ktor.util.KtorExperimentalAPI
import io.prometheus.client.hotspot.DefaultExports
import no.nav.helse.dusseldorf.ktor.auth.*
import no.nav.helse.dusseldorf.ktor.client.HttpRequestHealthConfig
import no.nav.helse.dusseldorf.ktor.core.*
import no.nav.helse.dusseldorf.ktor.health.HealthReporter
import no.nav.helse.dusseldorf.ktor.health.HealthService
import no.nav.helse.dusseldorf.ktor.jackson.JacksonStatusPages
import no.nav.helse.dusseldorf.ktor.jackson.dusseldorfConfigured
import no.nav.helse.dusseldorf.ktor.metrics.MetricsRoute
import no.nav.helse.dusseldorf.ktor.metrics.init
import no.nav.k9.aksjonspunktbehandling.K9sakEventHandler
import no.nav.k9.db.hikariConfig
import no.nav.k9.domene.repository.BehandlingProsessEventRepository
import no.nav.k9.domene.repository.OppgaveRepository
import no.nav.k9.integrasjon.rest.RequestContextService
import no.nav.k9.integrasjon.tps.TpsProxyV1
import no.nav.k9.integrasjon.tps.TpsProxyV1Gateway
import no.nav.k9.kafka.AsynkronProsesseringV1Service
import no.nav.k9.tjenester.admin.AdminApis
import no.nav.k9.tjenester.avdelingsleder.AvdelingslederApis
import no.nav.k9.tjenester.avdelingsleder.nøkkeltall.NøkkeltallApis
import no.nav.k9.tjenester.avdelingsleder.oppgave.AvdelingslederOppgaveApis
import no.nav.k9.tjenester.avdelingsleder.saksbehandler.AvdelingslederSaksbehandlerApis
import no.nav.k9.tjenester.avdelingsleder.saksliste.AvdelingslederSakslisteApis
import no.nav.k9.tjenester.kodeverk.HentKodeverkTjeneste
import no.nav.k9.tjenester.kodeverk.KodeverkApis
import no.nav.k9.tjenester.konfig.KonfigApis
import no.nav.k9.tjenester.mock.MockGrensesnitt
import no.nav.k9.tjenester.saksbehandler.NavAnsattApis
import no.nav.k9.tjenester.saksbehandler.nøkkeltall.SaksbehandlerNøkkeltallApis
import no.nav.k9.tjenester.saksbehandler.oppgave.OppgaveApis
import no.nav.k9.tjenester.saksbehandler.oppgave.OppgaveTjenesteImpl
import no.nav.k9.tjenester.saksbehandler.saksliste.SaksbehandlerSakslisteApis
import java.net.URI
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

    val tpsProxyV1Gateway = TpsProxyV1Gateway(
        tpsProxyV1 = TpsProxyV1(
            baseUrl = configuration.tpsProxyV1Url(),
            accessTokenClient = accessTokenClientResolver.naisSts()
        )
    )
    val dataSource = hikariConfig(configuration)
    val oppgaveRepository = OppgaveRepository(dataSource)
    val oppgaveTjeneste = OppgaveTjenesteImpl(oppgaveRepository, tpsProxyV1Gateway = tpsProxyV1Gateway)
    val behandlingProsessEventRepository = BehandlingProsessEventRepository(dataSource)
    val k9sakEventHandler = K9sakEventHandler(
        oppgaveRepository = oppgaveRepository,
        behandlingProsessEventRepository = behandlingProsessEventRepository
        //                        gosysOppgaveGateway = gosysOppgaveGateway
        , config = configuration
    )
    val asynkronProsesseringV1Service = AsynkronProsesseringV1Service(
        kafkaConfig = configuration.getKafkaConfig(),
        configuration = configuration,
        k9sakEventHandler = k9sakEventHandler
//        gosysOppgaveGateway = gosysOppgaveGateway
    )
//    val idTokenProvider = IdTokenProvider(cookieName = configuration.getCookieName())

    environment.monitor.subscribe(ApplicationStopping) {
        log.info("Stopper AsynkronProsesseringV1Service.")
        asynkronProsesseringV1Service.stop()
        log.info("AsynkronProsesseringV1Service Stoppet.")
    }
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
        route("mock") {
            MockGrensesnitt(k9sakEventHandler, behandlingProsessEventRepository)
        }
        authenticate(*issuers.allIssuers()) {

            route("api") {
                AdminApis()
                AvdelingslederApis()
                AvdelingslederOppgaveApis()
                AvdelingslederSaksbehandlerApis()
                AvdelingslederSakslisteApis()
                NøkkeltallApis()
                route("saksbehandler") {
                    route("oppgaver") {
                        OppgaveApis(
                            requestContextService,
                            oppgaveTjeneste,
                            tpsProxyV1Gateway = tpsProxyV1Gateway
                        )
                    }
                    SaksbehandlerSakslisteApis()
                    SaksbehandlerNøkkeltallApis()
                }
                NavAnsattApis()

                SaksbehandlerNøkkeltallApis()
                route("konfig") { KonfigApis() }
                KodeverkApis(kodeverkTjeneste = kodeverkTjeneste)
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


    install(CallId) {
        fromXCorrelationIdHeader()
    }

    intercept(ApplicationCallPipeline.Monitoring) {
        call.request.log()
    }

//    install(CallLogging) {
//        correlationIdAndRequestIdInMdc()
//        logRequests()
////        mdc("id_token_jti") { call ->
////            try { idTokenProvider.getIdToken(call).getId() }
////            catch (cause: Throwable) { null }
////        }
//    }


}

private fun Map<Issuer, Set<ClaimRule>>.healthCheckMap(
    initial: MutableMap<URI, HttpRequestHealthConfig>
): Map<URI, HttpRequestHealthConfig> {
    forEach { issuer, _ ->
        initial[issuer.jwksUri()] =
            HttpRequestHealthConfig(expectedStatus = HttpStatusCode.OK, includeExpectedStatusEntity = false)
    }
    return initial.toMap()
}