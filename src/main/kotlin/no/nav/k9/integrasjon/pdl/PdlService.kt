package no.nav.k9.integrasjon.pdl

import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpPost
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import no.nav.helse.dusseldorf.ktor.client.buildURL
import no.nav.helse.dusseldorf.ktor.core.Retry
import no.nav.helse.dusseldorf.ktor.metrics.Operation
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.k9.aksjonspunktbehandling.objectMapper
import no.nav.k9.domene.oppslag.Ident
import no.nav.k9.integrasjon.rest.*
import no.nav.k9.integrasjon.tps.TpsPerson
import no.nav.k9.integrasjon.tps.TpsProxyV1
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.Duration
import java.time.LocalDate
import java.util.*
import kotlin.coroutines.coroutineContext

class PdlService(
    baseUrl: URI,
    accessTokenClient: AccessTokenClient,
    private val henteNavnScopes: Set<String> = setOf("openid")
) {
    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(TpsProxyV1::class.java)
    }

    private val personUrl = Url.buildURL(
        baseUrl = baseUrl,
        pathParts = listOf()
    ).toString()

    internal suspend fun person(ident: Ident): TpsPerson {

        val httpRequest = personUrl
            .httpPost()
            .body(
                objectMapper().writeValueAsString(
                    QueryRequest(
                        """query(\$ident: ID!) {
    hentIdenter(ident: \$ident, historikk: false, grupper: [FOLKEREGISTERIDENT,AKTORID]){
        identer{
            ident
            gruppe
        }
    }
    hentPerson(ident: \$ident) {
        doedsfall { doedsdato }
        adressebeskyttelse(historikk: false) {
            gradering
        }
        bostedsadresse(historikk: false) {
            vegadresse {kommunenummer}
            matrikkeladresse{kommunenummer}
            ukjentBosted{bostedskommune}
        }
        sikkerhetstiltak {
            beskrivelse
        }
        navn(historikk: false) {
            fornavn
            mellomnavn
            etternavn
            metadata {
                master
            }
        }
    }
}""", mapOf("ident" to ident.value)
                    )
                )
            )
            .header(
                HttpHeaders.Authorization to "Bearer ${coroutineContext.idToken().value}",
                NavHeaders.ConsumerToken to "Bearer " + cachedAccessTokenClient.getAccessToken(henteNavnScopes)
                    .asAuthoriationHeader(),
                HttpHeaders.Accept to "application/json",
                NavHeaders.Tema to "OMS",
                NavHeaders.CallId to UUID.randomUUID().toString()
            )

        logger.restKall(personUrl)
        logger.info(httpRequest)
        val json = Retry.retry(
            operation = "hente-person",
            initialDelay = Duration.ofMillis(200),
            factor = 2.0,
            logger = logger
        ) {
            val (request, _, result) = Operation.monitored(
                app = "k9-los-api",
                operation = "hente-person",
                resultResolver = { 200 == it.second.statusCode }
            ) { httpRequest.awaitStringResponseResult() }

            result.fold(
                { success -> JSONObject(success) },
                { error ->
                    logger.error(
                        "Error response = '${error.response.body().asString("text/plain")}' fra '${request.url}'"
                    )
                    logger.error(error.toString())
                    throw IllegalStateException("Feil ved henting av person.")
                }
            )
        }

        logger.logResponse(json)

        val navn = json.getJSONObject("navn")


        return TpsPerson(
            fornavn = navn.getString("fornavn"),
            mellomnavn = navn.getStringOrNull("mellomnavn"),
            etternavn = navn.getString("slektsnavn"),
            fødselsdato = LocalDate.parse(json.getString("foedselsdato")),
            diskresjonskode = json.getString("diskresjonskode"),
            kjønn = json.getString("kjoenn"),
            dødsdato = LocalDate.parse(json.getString("doedsdato")),
            navn = """${navn.getString("fornavn")} ${navn.getString("slektsnavn")}""",
            ident = json.getString("ident")
        )
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
}
