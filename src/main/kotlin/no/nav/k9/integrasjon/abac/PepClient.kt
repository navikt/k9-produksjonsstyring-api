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
import no.nav.k9.integrasjon.audit.*
import no.nav.k9.integrasjon.azuregraph.AzureGraphService
import no.nav.k9.integrasjon.rest.NavHeaders
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

private val gson = GsonBuilder().setPrettyPrinting().create()

private const val XACML_CONTENT_TYPE = "application/xacml+json"
private const val DOMENE = "k9"

class PepClient @KtorExperimentalAPI constructor(
    private val azureGraphService: AzureGraphService,
    private val auditlogger: Auditlogger,
    private val config: Configuration
) {
    @KtorExperimentalAPI
    private val url = config.abacEndpointUrl
    private val log: Logger = LoggerFactory.getLogger(PepClient::class.java)

    @KtorExperimentalAPI
    suspend fun erOppgaveStyrer(): Boolean {
        val requestBuilder = XacmlRequestBuilder()
            .addResourceAttribute(RESOURCE_DOMENE, DOMENE)
            .addResourceAttribute(RESOURCE_TYPE, OPPGAVESTYRER)
            .addAccessSubjectAttribute(SUBJECT_TYPE, INTERNBRUKER)
            .addAccessSubjectAttribute(SUBJECTID, azureGraphService.hentIdentTilInnloggetBruker())
            .addEnvironmentAttribute(ENVIRONMENT_PEP_ID, "srvk9los")

        val decision = evaluate(requestBuilder)
        return decision
    }

    @KtorExperimentalAPI
    suspend fun harBasisTilgang(): Boolean {

        val requestBuilder = XacmlRequestBuilder()
            .addResourceAttribute(RESOURCE_DOMENE, DOMENE)
            .addResourceAttribute(RESOURCE_TYPE, BASIS_TILGANG)
            .addActionAttribute(ACTION_ID, "read")
            .addAccessSubjectAttribute(SUBJECT_TYPE, INTERNBRUKER)
            .addAccessSubjectAttribute(SUBJECTID, azureGraphService.hentIdentTilInnloggetBruker())
            .addEnvironmentAttribute(ENVIRONMENT_PEP_ID, "srvk9los")

        val decision = evaluate(requestBuilder)
        return decision
    }

    suspend fun hentIdentTilInnloggetBruker(): String {
        return azureGraphService.hentIdentTilInnloggetBruker()
    }

    @KtorExperimentalAPI
    suspend fun harTilgangTilLesSak(
        fagsakNummer: String
    ): Boolean {

        val identTilInnloggetBruker = azureGraphService.hentIdentTilInnloggetBruker()
        val requestBuilder = XacmlRequestBuilder()
            .addResourceAttribute(RESOURCE_DOMENE, DOMENE)
            .addResourceAttribute(RESOURCE_TYPE, LESETILGANG_SAK)
            .addActionAttribute(ACTION_ID, "read")
            .addAccessSubjectAttribute(SUBJECT_TYPE, INTERNBRUKER)
            .addAccessSubjectAttribute(SUBJECTID, identTilInnloggetBruker)
            .addEnvironmentAttribute(ENVIRONMENT_PEP_ID, "srvk9los")
            .addResourceAttribute(RESOURCE_SAKSNR, fagsakNummer)
        val decision = evaluate(requestBuilder)

        auditlogger.logg(
            Auditdata(
                header = AuditdataHeader(
                    vendor = auditlogger.defaultVendor,
                    product = auditlogger.defaultProduct,
                    eventClassId = EventClassId.AUDIT_SEARCH,
                    name = "ABAC Sporingslogg",
                    severity = "INFO"
                ), fields = setOf(
                    CefField(CefFieldName.EVENT_TIME, LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)*1000L),
                    CefField(CefFieldName.REQUEST, "read"),
                    CefField(CefFieldName.ABAC_RESOURCE_TYPE, LESETILGANG_SAK),
                    CefField(CefFieldName.ABAC_ACTION, "read"),
                    CefField(CefFieldName.USER_ID, identTilInnloggetBruker),
                    CefField(CefFieldName.BERORT_BRUKER_ID, "read"),

                    CefField(CefFieldName.BEHANDLING_VERDI, "behandlingsid"),
                    CefField(CefFieldName.BEHANDLING_LABEL, "Behandling"),
                    CefField(CefFieldName.SAKSNUMMER_VERDI, fagsakNummer),
                    CefField(CefFieldName.SAKSNUMMER_LABEL, "Saksnummer")
                )
            )
        )

        return decision
    }

    @KtorExperimentalAPI
    suspend fun kanSendeSakTilStatistikk(
        fagsakNummer: String
    ): Boolean {
        if (config.erLokalt()) {
            log.info("Lokal kjøring, får alltid true ved sjekk på om sak kan sendes til statistikk")
        }
        val requestBuilder = XacmlRequestBuilder()
            .addResourceAttribute(RESOURCE_DOMENE, DOMENE)
            .addResourceAttribute(RESOURCE_TYPE, LESETILGANG_SAK)
            .addActionAttribute(ACTION_ID, "read")
            .addAccessSubjectAttribute(SUBJECT_TYPE, KAFKATOPIC)
            .addAccessSubjectAttribute(SUBJECTID, KAFKATOPIC_STATISTIKK)
            .addEnvironmentAttribute(ENVIRONMENT_PEP_ID, "srvk9los")
            .addResourceAttribute(RESOURCE_SAKSNR, fagsakNummer)

        val decision = evaluate(requestBuilder)

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
            // log.info("abac result: $json \n\n $xacmlJson\n\n" + httpRequest.toString())
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
