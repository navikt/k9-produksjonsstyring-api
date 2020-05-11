package no.nav.k9.integrasjon.abac

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpPost
import com.google.gson.GsonBuilder
import io.ktor.http.HttpHeaders
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.helse.dusseldorf.ktor.core.Retry
import no.nav.helse.dusseldorf.ktor.metrics.Operation
import no.nav.k9.Configuration
import no.nav.k9.aksjonspunktbehandling.objectMapper
import no.nav.k9.integrasjon.azuregraph.AzureGraphService
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

class PepClient @KtorExperimentalAPI constructor(private val azureGraphService: AzureGraphService, private val config: Configuration) {
    @KtorExperimentalAPI
    private val url = config.abacEndpointUrl
    private val log: Logger = LoggerFactory.getLogger(PepClient::class.java)
        
    @KtorExperimentalAPI
    suspend fun erOppgaveStyrer(idToken: IdToken): Boolean {
        val cachedResponse = abacCache.hasAccess(idToken, OPPGAVESTYRER, OPPGAVESTYRER )
        if (cachedResponse != null) {
            return cachedResponse
        }
        val requestBuilder = XacmlRequestBuilder()
            .addEnvironmentAttribute(ENVIRONMENT_OIDC_TOKEN_BODY, idToken.value)
            .addResourceAttribute(RESOURCE_DOMENE, DOMENE)
            .addResourceAttribute(RESOURCE_TYPE, OPPGAVESTYRER)
            .addAccessSubjectAttribute(SUBJECT_TYPE, INTERNBRUKER)
            .addAccessSubjectAttribute(SUBJECTID, azureGraphService.hentIdentTilInnloggetBruker())
            .addEnvironmentAttribute(ENVIRONMENT_PEP_ID, "srvk9los")

        val decision = evaluate(requestBuilder)
        abacCache.storeResultOfLookup(idToken, OPPGAVESTYRER, OPPGAVESTYRER, decision)
        return decision
    }

    @KtorExperimentalAPI
    suspend fun harBasisTilgang(idToken: IdToken): Boolean {
        val cachedResponse = abacCache.hasAccess(idToken, BASIS_TILGANG, BASIS_TILGANG )
        if (cachedResponse != null) {
            return cachedResponse
        }
        
        val requestBuilder = XacmlRequestBuilder()
            .addResourceAttribute(RESOURCE_DOMENE, DOMENE)
            .addResourceAttribute(RESOURCE_TYPE, BASIS_TILGANG)
            .addActionAttribute(ACTION_ID, "read")
            .addAccessSubjectAttribute(SUBJECT_TYPE, INTERNBRUKER)
            .addAccessSubjectAttribute(SUBJECTID, azureGraphService.hentIdentTilInnloggetBruker())
            .addEnvironmentAttribute(ENVIRONMENT_PEP_ID, "srvk9los")

        val decision =  evaluate(requestBuilder)
        abacCache.storeResultOfLookup(idToken, OPPGAVESTYRER, OPPGAVESTYRER, decision)
        return decision
    }


    @KtorExperimentalAPI
    suspend fun harTilgangTilLesSak(
        idToken: IdToken,
        fagsakNummer: String
    ): Boolean {
        val cachedResponse = abacCache.hasAccess(idToken, LESETILGANG_SAK, "read" )
        if (cachedResponse != null) {
            return cachedResponse
        }

        val requestBuilder = XacmlRequestBuilder()
            .addResourceAttribute(RESOURCE_DOMENE, DOMENE)
            .addResourceAttribute(RESOURCE_TYPE, LESETILGANG_SAK)
            .addActionAttribute(ACTION_ID, "read")
            .addAccessSubjectAttribute(SUBJECT_TYPE, INTERNBRUKER)
            .addAccessSubjectAttribute(SUBJECTID, azureGraphService.hentIdentTilInnloggetBruker())
            .addEnvironmentAttribute(ENVIRONMENT_PEP_ID, "srvk9los")
            .addResourceAttribute(RESOURCE_SAKSNR, fagsakNummer)

        val decision =  evaluate(requestBuilder)
        abacCache.storeResultOfLookup(idToken, OPPGAVESTYRER, "read" , decision)
        return decision
    }


    @KtorExperimentalAPI
    private suspend fun evaluate(xacmlRequestBuilder: XacmlRequestBuilder): Boolean {
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
            log.info("abac result: $json \n\n $xacmlJson")
            try {
                objectMapper().readValue<Response>(json).response[0].decision == "Permit"
            } catch (e: Exception) {
                log.error(
                    "Feilet deserialisering", e
                )
                false
            }
        }
    }
   
}
