package no.nav.k9.integrasjon.pdl

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpPost
import info.debatty.java.stringsimilarity.Levenshtein
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.dusseldorf.ktor.client.buildURL
import no.nav.helse.dusseldorf.ktor.core.Retry
import no.nav.helse.dusseldorf.ktor.metrics.Operation
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.k9.Configuration
import no.nav.k9.aksjonspunktbehandling.objectMapper
import no.nav.k9.integrasjon.rest.NavHeaders
import no.nav.k9.integrasjon.rest.idToken
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.Duration
import java.util.*
import kotlin.coroutines.coroutineContext

class PdlService @KtorExperimentalAPI constructor(
    baseUrl: URI,
    accessTokenClient: AccessTokenClient,
    val configuration: Configuration,
    private val henteNavnScopes: Set<String> = setOf("openid")
) {
    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)

    companion object {
        fun getQ2Ident(aktorId: String): String {
            val q2 = listOf(
                "14128521632",
                "14088521472",
                "25078522014",
                "27078523633",
                "16018623009",
                "27078522688",
                "19128521618",
                "21078525115"
            )
            val levenshtein = Levenshtein()
            var dist = Integer.MAX_VALUE.toDouble();
            var newIdent = "14128521632"

            q2.forEach { i ->
                val distance = levenshtein.distance(i, aktorId)
                if (distance < dist) {
                    dist = distance
                    newIdent = i
                }
            }
            return newIdent
        }

        private val log: Logger = LoggerFactory.getLogger(PdlService::class.java)
    }

    private val personUrl = Url.buildURL(
        baseUrl = baseUrl,
        pathParts = listOf()
    ).toString()

    @KtorExperimentalAPI
    internal suspend fun person(aktorId: String): PersonPdl? {
        if (configuration.erLokalt()) {
            return PersonPdl(
                data = PersonPdl.Data(
                    hentPerson = PersonPdl.Data.HentPerson(
                        listOf(
                            element =
                            PersonPdl.Data.HentPerson.Folkeregisteridentifikator("012345678901")
                        ),
                        navn = listOf(
                            PersonPdl.Data.HentPerson.Navn(
                                etternavn = "Etternavn",
                                forkortetNavn = "ForkortetNavn",
                                fornavn = "Fornavn",
                                mellomnavn = null
                            )
                        )
                    )
                )
            )
        }
        val queryRequest = QueryRequest(
            getStringFromResource("/pdl/hentPerson.graphql"),
            mapOf("ident" to getQ2Ident(aktorId, configuration = configuration))
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
        return try {
            return objectMapper().readValue<PersonPdl>(json)
        } catch (e: Exception) {
            null
        }
    }

    @KtorExperimentalAPI
    private fun getQ2Ident(aktorId: String, configuration: Configuration): String {
        if (!configuration.erIDevFss()) {
            return aktorId
        }
        return getQ2Ident(aktorId)
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



