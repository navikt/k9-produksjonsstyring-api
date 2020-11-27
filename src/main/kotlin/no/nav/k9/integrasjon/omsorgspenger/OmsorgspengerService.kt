package no.nav.k9.integrasjon.omsorgspenger

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpPost
import io.ktor.http.*
import io.ktor.util.*
import no.nav.helse.dusseldorf.ktor.core.Retry
import no.nav.helse.dusseldorf.ktor.metrics.Operation
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.k9.Configuration
import no.nav.k9.NaisStsAccessTokenClient
import no.nav.k9.aksjonspunktbehandling.objectMapper
import no.nav.k9.integrasjon.rest.NavHeaders
import no.nav.k9.integrasjon.rest.idToken
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*
import kotlin.coroutines.coroutineContext


open class OmsorgspengerService @KtorExperimentalAPI constructor(
    val configuration: Configuration,
    val accessTokenClient: AccessTokenClient

) : IOmsorgspengerService {
    private val log: Logger = LoggerFactory.getLogger(OmsorgspengerService::class.java)

    @KtorExperimentalAPI
    private val url = configuration.omsorgspengerUrl()
    @KtorExperimentalAPI
    private var scope = configuration.omsorgspengerSakScope()

    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)

    @KtorExperimentalAPI
    override suspend fun hentOmsorgspengerSakDto(identitetsnummer: String): OmsorgspengerSakDto? {
        scope = "3ebacf0c-2409-4ae7-8507-07c8da9ddd25/.default";
        log.info("Fant dette scopet=  ${scope}")
        log.info("IdTokenet som er brukt: ${coroutineContext.idToken().value}")

        val httpRequest = "${url}/saksnummer"
            .httpPost()
            .body(
                identitetsnummer
            )
            .header(
                HttpHeaders.Authorization to "Bearer ${coroutineContext.idToken().value}",
                HttpHeaders.Accept to "application/json",
                HttpHeaders.ContentType to "application/json",
                NavHeaders.CallId to UUID.randomUUID().toString()
            )

        val json = Retry.retry(
            operation = "hent-saksnummer-omsorgspenger",
            initialDelay = Duration.ofMillis(200),
            factor = 2.0,
            logger = log
        ) {
            val (request, _, result) = Operation.monitored(
                app = "k9-los-api",
                operation = "hent-saksnummer-omsorgspenger",
                resultResolver = { 200 == it.second.statusCode }
            ) { httpRequest.awaitStringResponseResult() }

            result.fold(
                { success ->
                    success
                },
                { error ->
                    log.error(request.toString())
                    log.error(
                        "Error response = '${error.response.body().asString("text/plain")}' fra '${request.url}'"
                    )
                    log.error(error.toString())
                    null
                }
            )
        }
        return try {
            objectMapper().readValue(json!!)
        } catch (e: Exception) {
            log.warn("", e)
            null
        }
    }
}


