package no.nav.k9.tjenester.saksbehandler

import io.ktor.application.call
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.withContext
import no.nav.k9.Configuration
import no.nav.k9.domene.modell.Saksbehandler
import no.nav.k9.domene.repository.SaksbehandlerRepository
import no.nav.k9.integrasjon.abac.PepClient
import no.nav.k9.integrasjon.azuregraph.AzureGraphService
import no.nav.k9.integrasjon.rest.RequestContextService
import no.nav.k9.tjenester.avdelingsleder.InnloggetNavAnsattDto
import org.slf4j.LoggerFactory

@KtorExperimentalAPI
@KtorExperimentalLocationsAPI
internal fun Route.NavAnsattApis(
    pepClient: PepClient,
    requestContextService: RequestContextService,
    saksbehandlerRepository: SaksbehandlerRepository,
    azureGraphService: AzureGraphService,
    configuration: Configuration
) {
    @Location("/saksbehandler")
    class getInnloggetBruker

    val log = LoggerFactory.getLogger("Route.NavAnsattApis")

    get { _: getInnloggetBruker ->
        if (configuration.erIkkeLokalt) {
            val idtoken = call.idToken()
            withContext(
                requestContextService.getCoroutineContext(
                    context = coroutineContext,
                    idToken = idtoken
                )
            ) {
                val token = IdToken(idtoken.value)
                val innloggetNavAnsattDto = InnloggetNavAnsattDto(
                    token.getUsername(),
                    token.getName(),
                    kanSaksbehandle = pepClient.harBasisTilgang(token),
                    kanVeilede = pepClient.harBasisTilgang(token),
                    kanBeslutte = pepClient.harBasisTilgang(token),
                    kanBehandleKodeEgenAnsatt = pepClient.harBasisTilgang(token),
                    kanBehandleKode6 = pepClient.harBasisTilgang(token),
                    kanBehandleKode7 = pepClient.harBasisTilgang(token),
                    kanOppgavestyre = pepClient.erOppgaveStyrer(token)
                )
                if (saksbehandlerRepository.finnSaksbehandler(token.getUsername()) != null) {
                    saksbehandlerRepository.addSaksbehandler(
                        Saksbehandler(
                            brukerIdent = azureGraphService.hentIdentTilInnloggetBruker(),
                            navn = token.getName(),
                            epost = token.getUsername().toLowerCase()
                        )
                    )
                }
                call.respond(
                    innloggetNavAnsattDto
                )
            }
        } else {
            call.respond(
                InnloggetNavAnsattDto(
                    "alexaban",
                    "Saksbehandler Sara",
                    kanSaksbehandle = true,
                    kanVeilede = true,
                    kanBeslutte = true,
                    kanBehandleKodeEgenAnsatt = true,
                    kanBehandleKode6 = true,
                    kanBehandleKode7 = true,
                    kanOppgavestyre = true
                )
            )
        }
    }
}