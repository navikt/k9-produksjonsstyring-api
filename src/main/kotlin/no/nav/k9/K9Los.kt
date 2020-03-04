package no.nav.k9

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.application.*
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.features.CallId
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.http.HttpStatusCode
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
import no.nav.helse.dusseldorf.ktor.jackson.JacksonStatusPages
import no.nav.helse.dusseldorf.ktor.jackson.dusseldorfConfigured
import no.nav.helse.dusseldorf.ktor.metrics.MetricsRoute
import no.nav.helse.dusseldorf.ktor.metrics.init
import no.nav.k9.db.hikariConfig
import no.nav.k9.domene.repository.BehandlingProsessEventRepository
import no.nav.k9.domene.repository.OppgaveRepository
import no.nav.k9.integrasjon.gosys.GosysOppgaveGateway
import no.nav.k9.kafka.AsynkronProsesseringV1Service
import no.nav.k9.tjenester.admin.AdminApis
import no.nav.k9.tjenester.avdelingsleder.AvdelingslederApis
import no.nav.k9.tjenester.saksbehandler.NavAnsattApis
import no.nav.k9.tjenester.avdelingsleder.nøkkeltall.NøkkeltallApis
import no.nav.k9.tjenester.avdelingsleder.oppgave.AvdelingslederOppgaveApis
import no.nav.k9.tjenester.avdelingsleder.saksbehandler.AvdelingslederSaksbehandlerApis
import no.nav.k9.tjenester.avdelingsleder.saksliste.AvdelingslederSakslisteApis
import no.nav.k9.tjenester.saksbehandler.nøkkeltall.SaksbehandlerNøkkeltallApis
import no.nav.k9.tjenester.saksbehandler.oppgave.OppgaveApis
import no.nav.k9.tjenester.saksbehandler.saksliste.SaksbehandlerSakslisteApis
import org.apache.http.impl.client.HttpClients
import no.nav.k9.tjenester.kodeverk.HentKodeverkTjeneste
import no.nav.k9.tjenester.kodeverk.KodeverkApis
import no.nav.k9.tjenester.konfig.KonfigApis
import no.nav.k9.tjenester.saksbehandler.oppgave.OppgaveTjenesteImpl
import java.net.URI

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

//    val accessTokenClientResolver = AccessTokenClientResolver(
//        clients = configuration.clients()
//    )

//    val gosysOppgaveGateway =
//        GosysOppgaveGateway(
//            httpClient = HttpClients.createSystem(),
//            uri = configuration.getOppgaveBaseUri(),
//            accessTokenClientResolver = accessTokenClientResolver
//        )

//    val healthService = HealthService(setOf(
//            journalforingGateway,
//            dokumentGateway,
//            HttpRequestHealthCheck(
//                    urlConfigMap = issuers.healthCheckMap(mutableMapOf(
//                            Url.buildURL(baseUrl = configuration.getDokarkivBaseUrl(), pathParts = listOf("isReady")) to HttpRequestHealthConfig(expectedStatus = HttpStatusCode.OK)
//                    ))
//            ))
//    )

    val dataSource = hikariConfig(configuration)
    val oppgaveRepository = OppgaveRepository(dataSource)
    val oppgaveTjeneste = OppgaveTjenesteImpl(oppgaveRepository)
    val behandlingProsessEventRepository = BehandlingProsessEventRepository(dataSource)
    val asynkronProsesseringV1Service = AsynkronProsesseringV1Service(
        kafkaConfig = configuration.getKafkaConfig(),
        oppgaveRepository = oppgaveRepository,
        behandlingProsessEventRepository = behandlingProsessEventRepository
//        gosysOppgaveGateway = gosysOppgaveGateway
    )

    environment.monitor.subscribe(ApplicationStopping) {
        log.info("Stopper AsynkronProsesseringV1Service.")
        asynkronProsesseringV1Service.stop()
        log.info("AsynkronProsesseringV1Service Stoppet.")
    }

    install(CallIdRequired)

    install(Locations)

    install(Routing) {

        val kodeverkTjeneste = HentKodeverkTjeneste()

        authenticate(*issuers.allIssuers()) {
            requiresCallId {

            }
        }
        MetricsRoute()
        DefaultProbeRoutes()

        route("api") {
            AdminApis()
            AvdelingslederApis()
            AvdelingslederOppgaveApis()
            AvdelingslederSaksbehandlerApis()
            AvdelingslederSakslisteApis()
            NøkkeltallApis()
            route("saksbehandler") {
                OppgaveApis(oppgaveTjeneste)
                SaksbehandlerSakslisteApis()
                SaksbehandlerNøkkeltallApis()
            }
            NavAnsattApis()

            SaksbehandlerNøkkeltallApis()
            route("konfig") { KonfigApis() }
            KodeverkApis(kodeverkTjeneste = kodeverkTjeneste)
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

    install(CallLogging) {
        correlationIdAndRequestIdInMdc()
        logRequests()
    }
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
