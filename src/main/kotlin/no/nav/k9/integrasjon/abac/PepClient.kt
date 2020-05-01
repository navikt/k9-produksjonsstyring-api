package no.nav.k9.integrasjon.abac

import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpPost
import com.google.gson.GsonBuilder
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.helse.dusseldorf.ktor.core.Retry
import no.nav.helse.dusseldorf.ktor.metrics.Operation
import no.nav.k9.Configuration
import no.nav.k9.integrasjon.rest.NavHeaders
import no.nav.k9.tjenester.saksbehandler.IdToken
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*

private val gson = GsonBuilder().setPrettyPrinting().create()
private val abacCache = AbacCache()

private const val XACML_CONTENT_TYPE = "application/xacml+json"
private const val DOMENE = "k9"

class PepClient(private val config: Configuration, private val bias: Decision) {
    private val url = config.abacEndpointUrl
    private val log: Logger = LoggerFactory.getLogger(PepClient::class.java)
        
    suspend fun erOppgaveStyrer(idToken: IdToken): Boolean {
        val cachedResponse = abacCache.hasAccess(idToken, OPPGAVESTYRER, OPPGAVESTYRER )
        if (cachedResponse != null) {
            return cachedResponse
        }
        XacmlRequestBuilder()
            .addEnvironmentAttribute(ENVIRONMENT_OIDC_TOKEN_BODY, idToken.value)
            .addResourceAttribute(RESOURCE_DOMENE, DOMENE)
            .addResourceAttribute(RESOURCE, OPPGAVESTYRER)
            .addAccessSubjectAttribute(SUBJECT_TYPE, INTERNBRUKER)

        val response = evaluate(createRequestWithDefaultHeaders(idToken.value, OPPGAVESTYRER))
        val decision = createBiasedDecision(response.getDecision()) == Decision.Permit
        abacCache.storeResultOfLookup(idToken, OPPGAVESTYRER, OPPGAVESTYRER, decision)
        return decision
    }

    private suspend fun evaluate(xacmlRequestBuilder: XacmlRequestBuilder): XacmlResponseWrapper {
        val xacmlJson = gson.toJson(xacmlRequestBuilder.build())
        return withContext(Dispatchers.IO) {
            val httpRequest = url
                .httpPost()
                .authentication()
                .basic(config.abacUsername, config.abacPassword)
                .body(
                    xacmlJson
                )
                .header(
                    HttpHeaders.Accept to "application/json",
                    HttpHeaders.ContentType to XACML_CONTENT_TYPE,
                    NavHeaders.CallId to UUID.randomUUID().toString()
                )

            val json = Retry.retry(
                operation = "evaluer abac",
                initialDelay = Duration.ofMillis(200),
                factor = 2.0,
                logger = log
            ) {
                val (request, _, result) = Operation.monitored(
                    app = "k9-los-api",
                    operation = "evaluate abac",
                    resultResolver = { 200 == it.second.statusCode }
                ) { httpRequest.awaitStringResponseResult() }

                result.fold(
                    { success -> success },
                    { error ->
                        log.error(
                            "Error response = '${error.response.body().asString("text/plain")}' fra '${request.url}'"
                        )
                        log.error(error.toString())
                        throw IllegalStateException("Feil ved evaluering av abac.")
                    }
                )
            }
            
            
            
            
//            val result = abacClient.post<HttpResponse>(url) {
//                body = TextContent(xacmlJson, ContentType.parse(XACML_CONTENT_TYPE))
//            }
//            if (result.status.value != 200) {
//                throw RuntimeException("ABAC call failed with ${result.status.value}")
//            }
//            val res = result.readText()
            log.info("Abac: $json")
            XacmlResponseWrapper(json)
        }
    }

    private fun createRequestWithDefaultHeaders(oidcTokenBody: String, action: String): XacmlRequestBuilder =
            XacmlRequestBuilder()
                .addEnvironmentAttribute(ENVIRONMENT_OIDC_TOKEN_BODY, oidcTokenBody)
                .addResourceAttribute(RESOURCE_DOMENE, DOMENE)
                .addActionAttribute(ACTION_ID, action)


    private fun createBiasedDecision(decision: Decision): Decision =
            when (decision) {
                Decision.NotApplicable, Decision.Indeterminate -> bias
                else -> decision
            }



    private fun extractBodyFromOidcToken(token: String): String =
            token.substringAfter(".").substringBefore(".")

}
