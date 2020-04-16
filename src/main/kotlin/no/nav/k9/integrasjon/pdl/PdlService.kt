package no.nav.k9.integrasjon.pdl

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpPost
import info.debatty.java.stringsimilarity.Cosine
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import no.nav.helse.dusseldorf.ktor.client.buildURL
import no.nav.helse.dusseldorf.ktor.core.Retry
import no.nav.helse.dusseldorf.ktor.metrics.Operation
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.k9.aksjonspunktbehandling.objectMapper
import no.nav.k9.domene.oppslag.Ident
import no.nav.k9.integrasjon.rest.NavHeaders
import no.nav.k9.integrasjon.rest.idToken
import no.nav.k9.integrasjon.rest.restKall
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.Duration
import java.util.*
import kotlin.coroutines.coroutineContext

class PdlService(
    baseUrl: URI,
    accessTokenClient: AccessTokenClient,
    private val henteNavnScopes: Set<String> = setOf("openid")
) {
    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)

    private companion object {
        private val log: Logger = LoggerFactory.getLogger(PdlService::class.java)
    }

    private val personUrl = Url.buildURL(
        baseUrl = baseUrl,
        pathParts = listOf()
    ).toString()

    internal suspend fun person(ident: Ident): PersonPdl {

        val queryRequest = QueryRequest(
            getStringFromResource("/pdl/hentPerson.graphql"),
            mapOf("ident" to getQ2Ident(ident))
        )

        val httpRequest = personUrl
            .httpPost()
            .body(
                objectMapper().writeValueAsString(
                    queryRequest
                )
            )
            .header(
                HttpHeaders.Authorization to "Bearer ${coroutineContext.idToken().value}",
                NavHeaders.ConsumerToken to cachedAccessTokenClient.getAccessToken(henteNavnScopes)
                    .asAuthoriationHeader(),
                HttpHeaders.Accept to "application/json",
                HttpHeaders.ContentType to "application/json",
                NavHeaders.Tema to "OMS",
                NavHeaders.CallId to UUID.randomUUID().toString()
            )

        log.restKall(personUrl)
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
                    log.error(
                        "Error response = '${error.response.body().asString("text/plain")}' fra '${request.url}'"
                    )
                    log.error(error.toString())
                    throw IllegalStateException("Feil ved henting av person.")
                }
            )
        }

        return objectMapper().readValue(json)
    }

    private fun getQ2Ident(ident: Ident): String {
        val q2 = listOf<String>(
            "14128521632",
            "14088521472",
            "25078522014",
            "27078523633",
            "16018623009",
            "27078522688",
            "19128521618",
            "21078525115"
        )
        val cosine = Cosine()
        var dist = 0.0;
        var newIdent = ident.value

        q2.forEach { i ->
            val distance = cosine.distance(i, ident.value)
            if (distance > dist) {
                dist = distance
                newIdent = i
            }
        }
        return newIdent
    }

    data class QueryRequest(
        val query: String,
        val variables: Map<String, String>,
        val operationName: String? = null
    ) {
        data class Variables(
            val variables: Map<String, Any>
        )
    }

    private fun getStringFromResource(path: String) =
        PdlService::class.java.getResourceAsStream(path).bufferedReader().use { it.readText() }

}



