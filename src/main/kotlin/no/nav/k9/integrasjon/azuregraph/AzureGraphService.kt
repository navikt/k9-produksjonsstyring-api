package no.nav.k9.integrasjon.azuregraph

import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpGet
import io.ktor.http.HttpHeaders
import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.dusseldorf.ktor.core.Retry
import no.nav.helse.dusseldorf.ktor.metrics.Operation
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.k9.Configuration
import no.nav.k9.integrasjon.rest.idToken
import org.slf4j.LoggerFactory
import java.time.Duration

class AzureGraphService @KtorExperimentalAPI constructor(
    accessTokenClient: AccessTokenClient,
    val configuration: Configuration
) {
    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)
    val log = LoggerFactory.getLogger("AzureGraphService")
    @KtorExperimentalAPI
    internal suspend fun hentIdentTilInnloggetBruker(): String {
        if (configuration.erLokalt) {
            return "" 
        }
        val accessToken =
            cachedAccessTokenClient.getAccessToken(setOf("https://graph.microsoft.com/user.read"), kotlin.coroutines.coroutineContext.idToken().value)

        val httpRequest = "https://graph.microsoft.com/v1.0/users/me?\$select=onPremisesSamAccountName"
            .httpGet()
            .header(
                HttpHeaders.Accept to "application/json",
                HttpHeaders.Authorization to "Bearer ${accessToken.token}"
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


        return "z994048"
    }
}



