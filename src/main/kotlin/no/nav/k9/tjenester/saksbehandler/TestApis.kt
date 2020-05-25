package no.nav.k9.tjenester.saksbehandler

import io.ktor.application.call
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.withContext
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.k9.AccessTokenClientResolver
import no.nav.k9.Configuration
import no.nav.k9.integrasjon.abac.PepClient
import no.nav.k9.integrasjon.pdl.PdlService
import no.nav.k9.integrasjon.rest.RequestContextService
import no.nav.k9.integrasjon.rest.idToken
import org.slf4j.LoggerFactory

@KtorExperimentalAPI
@KtorExperimentalLocationsAPI
internal fun Route.TestApis(
    requestContextService: RequestContextService,
    pdlService: PdlService,
    accessTokenClientResolver: AccessTokenClientResolver,
    accessTokenClient: AccessTokenClient,
    configuration: Configuration,
    pepClient: PepClient
) {

    val log = LoggerFactory.getLogger("Route.TestApis")
 
    @Location("/testToken")
    class getInnloggetBrukerToken
    get { _: getInnloggetBrukerToken ->
        val idtoken = call.idToken()
        withContext(
            requestContextService.getCoroutineContext(
                context = coroutineContext,
                idToken = idtoken
            )
        ) {
            call.respond(
                "id: " + idtoken.ident.value + "\n"
                        + "token: " + idtoken.value
                        + "\nnaistoken: " + accessTokenClientResolver.naisSts()
                    .getAccessToken(setOf("openid")).accessToken + "\n"
                        + "azuretoken: " + accessTokenClientResolver.accessTokenClient()
                    .getAccessToken(setOf("api://${accessTokenClientResolver.azureClientId()}/.default")).accessToken
            )
        }
    }

    @Location("/test")
    class getInnloggetBruker

    get { _: getInnloggetBruker ->
        withContext(
            requestContextService.getCoroutineContext(
                context = coroutineContext,
                idToken = call.idToken()
            )
        ) {
              val erOppgaveStyrer = pepClient.erOppgaveStyrer()

              val harbasistilgang = pepClient.harBasisTilgang()

            val accessToken =
                accessTokenClient.getAccessToken(
                    setOf("https://graph.microsoft.com/.default"),
                    kotlin.coroutines.coroutineContext.idToken().value
                )
            val harTilgangTilLesSak = pepClient.harTilgangTilLesSak("60HFW")
            
            call.respond(
                "${call.idToken().getUsername()} erOppgavestyrer: $erOppgaveStyrer, harBasistilgang: $harbasistilgang, har tilgang til 60HFW:$harTilgangTilLesSak"
            )
        }
    }
}