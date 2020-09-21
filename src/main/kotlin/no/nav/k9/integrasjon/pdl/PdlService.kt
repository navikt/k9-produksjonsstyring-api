package no.nav.k9.integrasjon.pdl

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpPost
import io.ktor.http.*
import io.ktor.util.*
import no.nav.helse.dusseldorf.ktor.client.buildURL
import no.nav.helse.dusseldorf.ktor.core.Retry
import no.nav.helse.dusseldorf.ktor.metrics.Operation
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.k9.Configuration
import no.nav.k9.aksjonspunktbehandling.objectMapper
import no.nav.k9.integrasjon.rest.NavHeaders
import no.nav.k9.integrasjon.rest.idToken
import no.nav.k9.utils.Cache
import no.nav.k9.utils.CacheObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.Duration
import java.time.LocalDateTime
import java.util.*
import kotlin.coroutines.coroutineContext

class PdlService @KtorExperimentalAPI constructor(
    baseUrl: URI,
    accessTokenClient: AccessTokenClient,
    val configuration: Configuration,
    private val henteNavnScopes: Set<String> = setOf("openid")
) : IPdlService {
    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)
    private val cache = Cache<String>(10_000)
    private val log: Logger = LoggerFactory.getLogger(PdlService::class.java)

    private val personUrl = Url.buildURL(
        baseUrl = baseUrl,
        pathParts = listOf()
    ).toString()

    @KtorExperimentalAPI
    override suspend fun person(aktorId: String): PersonPdlResponse {
        val queryRequest = QueryRequest(
            getStringFromResource("/pdl/hentPerson.graphql"),
            mapOf("ident" to aktorId)
        )
        val query = objectMapper().writeValueAsString(
            queryRequest
        )
        val cachedObject = cache.get(query)
        if (cachedObject == null) {
            val callId = UUID.randomUUID().toString()
            val httpRequest = personUrl
                .httpPost()
                .body(
                    query
                )
                .header(
                    HttpHeaders.Authorization to "Bearer ${coroutineContext.idToken().value}",
                    NavHeaders.ConsumerToken to cachedAccessTokenClient.getAccessToken(henteNavnScopes)
                        .asAuthoriationHeader(),
                    HttpHeaders.Accept to "application/json",
                    HttpHeaders.ContentType to "application/json",
                    NavHeaders.Tema to "OMS",
                    NavHeaders.CallId to callId
                )

            val json = Retry.retry(
                operation = "hente-person",
                initialDelay = Duration.ofMillis(200),
                factor = 2.0,
                logger = log
            ) {
                val (request, _, result) = Operation.monitored(
                    app = "k9-los-api",
                    operation = "hente-person",
                    resultResolver = { 200 == it.second.statusCode }
                ) { httpRequest.awaitStringResponseResult() }

                result.fold(
                    { success -> success },
                    { error ->
                        log.warn(
                            "Error response = '${error.response.body().asString("text/plain")}' fra '${request.url}'"
                        )
                        log.warn(error.toString() + "aktorId callId: " + callId + " " + coroutineContext.idToken().getUsername())
                        null
                    }
                )
            }
             try {
                val readValue = objectMapper().readValue<PersonPdl>(json!!)
                cache.set(query, CacheObject(json, LocalDateTime.now().plusHours(7)))
                return PersonPdlResponse(false, readValue)
            } catch (e: Exception) {
                try {
                    val value = objectMapper().readValue<Error>(json!!)
                    log.warn(objectMapper().writeValueAsString(value) + "aktorId callId: " + callId + " " + coroutineContext.idToken().getUsername())
                    if (value.errors.any { it.extensions.code == "unauthorized" }){
                        return PersonPdlResponse(true, null)
                    }
                } catch (e: Exception) {
                    log.warn("", e)
                }
                return PersonPdlResponse(false, null)
            }
        } else {
            return PersonPdlResponse(false, objectMapper().readValue<PersonPdl>(cachedObject.value))
        }
    }

    @KtorExperimentalAPI
    override suspend fun identifikator(fnummer: String): PdlResponse {
        val queryRequest = QueryRequest(
            getStringFromResource("/pdl/hentIdent.graphql"),
            mapOf(
                "ident" to fnummer,
                "historikk" to "false",
                "grupper" to listOf("AKTORID")
            )
        )
        val query = objectMapper().writeValueAsString(
            queryRequest
        )
        
        val cachedObject = cache.get(query)
        if (cachedObject == null) {
            val callId = UUID.randomUUID().toString()
            val httpRequest = personUrl
                .httpPost()
                .body(
                    query
                )
                .header(
                    HttpHeaders.Authorization to "Bearer ${coroutineContext.idToken().value}",
                    NavHeaders.ConsumerToken to cachedAccessTokenClient.getAccessToken(henteNavnScopes)
                        .asAuthoriationHeader(),
                    HttpHeaders.Accept to "application/json",
                    HttpHeaders.ContentType to "application/json",
                    NavHeaders.Tema to "OMS",
                    NavHeaders.CallId to callId
                )

            val json = Retry.retry(
                operation = "hente-ident",
                initialDelay = Duration.ofMillis(200),
                factor = 2.0,
                logger = log
            ) {
                val (request, _, result) = Operation.monitored(
                    app = "k9-los-api",
                    operation = "hente-ident",
                    resultResolver = { 200 == it.second.statusCode }
                ) { httpRequest.awaitStringResponseResult() }

                result.fold(
                    { success -> success },
                    { error ->
                        log.warn(
                            "Error response = '${error.response.body().asString("text/plain")}' fra '${request.url}'"
                        )
                        log.warn(error.toString())
                        null
                    }
                )
            }
            try {
                cache.set(query, CacheObject(json!!, LocalDateTime.now().plusDays(7)))
                return PdlResponse(false, objectMapper().readValue<AktøridPdl>(json))
            } catch (e: Exception) {
                try {
                    val value = objectMapper().readValue<Error>(json!!)
                    log.warn(objectMapper().writeValueAsString(value) + "identifikator callId: " + callId + " " + coroutineContext.idToken().getUsername())
                    if (value.errors.any { it.extensions.code == "unauthorized" }){
                        return PdlResponse(true, null)
                    }
                } catch (e: Exception) {
                    log.warn("", e)
                }
                return PdlResponse(false, null)
            }
        } else {
            return PdlResponse(false, objectMapper().readValue<AktøridPdl>(cachedObject.value))
        }
    }

    data class QueryRequest(
        val query: String,
        val variables: Map<String, Any>,
        val operationName: String? = null
    ) {
        data class Variables(
            val variables: Map<String, Any>
        )
    }

    private fun getStringFromResource(path: String) =
        PdlService::class.java.getResourceAsStream(path).bufferedReader().use { it.readText() }

}



