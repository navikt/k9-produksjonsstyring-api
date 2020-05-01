package no.nav.k9.tjenester.saksbehandler

import io.ktor.application.call
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.withContext
import no.nav.k9.AccessTokenClientResolver
import no.nav.k9.Configuration
import no.nav.k9.integrasjon.abac.PepClient
import no.nav.k9.integrasjon.pdl.PdlService
import no.nav.k9.integrasjon.rest.RequestContextService
import org.slf4j.LoggerFactory

@KtorExperimentalAPI
@KtorExperimentalLocationsAPI
internal fun Route.TestApis(
    requestContextService: RequestContextService,
    pdlService: PdlService,
    accessTokenClientResolver: AccessTokenClientResolver,
    configuration: Configuration,
    pepClient: PepClient
) {

    val log = LoggerFactory.getLogger("Route.TestApis")
    val gruppenavnSaksbehandler = "0000-GA-k9sak-saksbehandler"
    val gruppenavnVeileder = "0000-GA-k9sak-veileder"
    val gruppenavnBeslutter = "0000-GA-k9sak-beslutter"
    val gruppenavnEgenAnsatt = "0000-GA-GOSYS_UTVIDET"
    val gruppenavnKode6 = "0000-GA-GOSYS_KODE6"
    val gruppenavnKode7 = "0000-GA-GOSYS_KODE7"
    val gruppenavnOppgavestyrer = "0000-GA-k9sak-Oppgavestyrer"


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
            val erOppgaveStyrer = pepClient.erOppgaveStyrer(call.idToken())
            val harbasistilgang = pepClient.harBasisTilgang(call.idToken())
            call.respond(
                "erOppgavestyrer: $erOppgaveStyrer harBasistilgang $harbasistilgang"
//                tilgangskontroll.check(Policies.tilgangTilKode6.with("6"))
//                    .getDecision().decision == DecisionEnums.PERMIT
            )
        }
    }
}