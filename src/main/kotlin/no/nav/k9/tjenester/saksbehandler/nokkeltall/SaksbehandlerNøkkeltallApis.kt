package no.nav.k9.tjenester.saksbehandler.nokkeltall

import io.ktor.application.call
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.withContext
import no.nav.k9.KoinProfile
import no.nav.k9.integrasjon.rest.IRequestContextService
import no.nav.k9.tjenester.saksbehandler.IdTokenLocal
import no.nav.k9.tjenester.saksbehandler.idToken
import no.nav.k9.tjenester.saksbehandler.oppgave.OppgaveTjeneste
import org.koin.ktor.ext.inject

@KtorExperimentalAPI
@KtorExperimentalLocationsAPI
fun Route.SaksbehandlerNøkkeltallApis() {
    val requestContextService by inject<IRequestContextService>()
    val oppgaveTjeneste by inject<OppgaveTjeneste>()
    val profile by inject<KoinProfile>()
    
    @Location("/nokkeltall/nye-og-ferdigstilte-oppgaver")
    class getNyeOgFerdigstilteOppgaver

    get { _: getNyeOgFerdigstilteOppgaver ->
        withContext(
            requestContextService.getCoroutineContext(
                context = coroutineContext,
                idToken = if (profile != KoinProfile.LOCAL) {
                    call.idToken()
                } else {
                    IdTokenLocal()
                }
            )
        ) {
            call.respond(oppgaveTjeneste.hentNyeOgFerdigstilteOppgaver())
        }
    }
}
