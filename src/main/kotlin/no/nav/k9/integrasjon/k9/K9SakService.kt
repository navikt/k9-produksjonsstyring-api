package no.nav.k9.integrasjon.k9

import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpPost
import io.ktor.http.*
import io.ktor.util.*
import no.nav.helse.dusseldorf.ktor.core.Retry
import no.nav.helse.dusseldorf.ktor.metrics.Operation
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.k9.Configuration
import no.nav.k9.aksjonspunktbehandling.objectMapper
import no.nav.k9.sak.kontrakt.behandling.BehandlingIdListe
import no.nav.k9.utils.Cache
import org.slf4j.LoggerFactory
import java.time.Duration

open class K9SakService @KtorExperimentalAPI constructor(
    val configuration: Configuration,
    accessTokenClient: AccessTokenClient
) {
    val log = LoggerFactory.getLogger("K9SakService")
    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)
    private val cache = Cache<String>()
    suspend fun hentIdentTilInnloggetBruker(behandlingIdList: BehandlingIdListe) {
        val body = objectMapper().writeValueAsString(behandlingIdList)
        
        val httpRequest = "${configuration.k9Url()}/behandling/backend-root/refresh"
            .httpPost()
            .body(
                body
            )
            .header(
                HttpHeaders.Authorization to "Bearer ${cachedAccessTokenClient.getAccessToken(emptySet()).asAuthoriationHeader()}",
                HttpHeaders.Accept to "application/json",
                HttpHeaders.ContentType to "application/json"
            )

        val json = Retry.retry(
            operation = "hent-ident",
            initialDelay = Duration.ofMillis(200),
            factor = 2.0,
            logger = log
        ) {
            val (request, _, result) = Operation.monitored(
                app = "k9-los-api",
                operation = "hent-ident",
                resultResolver = { 200 == it.second.statusCode }
            ) { httpRequest.awaitStringResponseResult() }

            result.fold(
                { success -> success },
                { error ->
                    log.error(
                        "Error response = '${error.response.body().asString("text/plain")}' fra '${request.url}'"
                    )
                    log.error(error.toString())
                    throw IllegalStateException("Feil ved henting av saksbehandlers id")
                }
            )
        }
        log.info(json)
    }
}