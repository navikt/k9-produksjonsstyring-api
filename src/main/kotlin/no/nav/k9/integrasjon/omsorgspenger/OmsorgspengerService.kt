package no.nav.k9.integrasjon.omsorgspenger

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpPost
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import no.nav.helse.dusseldorf.ktor.core.Retry
import no.nav.helse.dusseldorf.ktor.metrics.Operation
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.k9.Configuration
import no.nav.k9.NaisStsAccessTokenClient
import no.nav.k9.aksjonspunktbehandling.objectMapper
import no.nav.k9.domene.lager.oppgave.Oppgave
import no.nav.k9.integrasjon.rest.NavHeaders
import no.nav.k9.integrasjon.rest.idToken
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*
import kotlin.coroutines.coroutineContext


open class OmsorgspengerService @KtorExperimentalAPI constructor(
    val configuration: Configuration,
    val accessTokenClient: AccessTokenClient,
    private val httpClient: HttpClient

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


        return kotlin.runCatching {
            httpClient.post<HttpStatement>("${url}/saksnummer") {
                header(HttpHeaders.Authorization, "Bearer ${coroutineContext.idToken().value}")
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                header(HttpHeaders.XCorrelationId, UUID.randomUUID().toString())
                body = identitetsnummer
            }.execute()
        }.håndterResponse()
    }


    private suspend fun Result<HttpResponse>.håndterResponse(): OmsorgspengerSakDto? = fold(
        onSuccess = { response ->
            return try {
                response.receive()
            } catch (e: Exception) {
                log.warn("", e)
                null
            }

        },
        onFailure = { cause ->
            when (cause is ResponseException) {
                true -> {
                    cause.response!!.logError()
                    throw RuntimeException("Uventet feil ved tilgangssjekk")
                }
                else -> throw cause
            }
        }
    )

    private suspend fun HttpResponse.logError() =
        log.error("HTTP ${status.value} fra omsorgspenger-tilgangsstyring, response: ${String(content.toByteArray())}")
}


