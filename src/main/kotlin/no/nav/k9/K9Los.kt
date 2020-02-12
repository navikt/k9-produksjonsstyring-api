package no.nav.k9

import io.ktor.application.Application
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.features.CallId
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.metrics.micrometer.MicrometerMetrics
import io.ktor.routing.Routing
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
import no.nav.k9.domene.repository.OppgaveRepository
import no.nav.k9.kafka.AsynkronProsesseringV1Service
import java.net.URI

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@KtorExperimentalAPI
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

//    val healthService = HealthService(setOf(
//            journalforingGateway,
//            dokumentGateway,
//            HttpRequestHealthCheck(
//                    urlConfigMap = issuers.healthCheckMap(mutableMapOf(
//                            Url.buildURL(baseUrl = configuration.getDokarkivBaseUrl(), pathParts = listOf("isReady")) to HttpRequestHealthConfig(expectedStatus = HttpStatusCode.OK)
//                    ))
//            ))
//    )

    val dataSource = hikariConfig()
    val oppgaveRepository = OppgaveRepository(dataSource)
    val asynkronProsesseringV1Service = AsynkronProsesseringV1Service(
        kafkaConfig = configuration.getKafkaConfig(),
        oppgaveRepository = oppgaveRepository
    )
    install(CallIdRequired)

    install(Routing) {
        authenticate(*issuers.allIssuers()) {
            requiresCallId {

            }
        }
        MetricsRoute()
        DefaultProbeRoutes()

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
    initial : MutableMap<URI, HttpRequestHealthConfig>
) : Map<URI, HttpRequestHealthConfig> {
    forEach { issuer, _ ->
        initial[issuer.jwksUri()] = HttpRequestHealthConfig(expectedStatus = HttpStatusCode.OK, includeExpectedStatusEntity = false)
    }
    return initial.toMap()
}
