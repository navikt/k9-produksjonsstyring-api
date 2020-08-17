package no.nav.k9.tjenester.saksbehandler.nokkeltall

import io.ktor.application.call
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.response.respond
import io.ktor.routing.Route
import kotlinx.coroutines.withContext
import no.nav.k9.Configuration
import no.nav.k9.domene.modell.BehandlingType
import no.nav.k9.integrasjon.rest.RequestContextService
import no.nav.k9.tjenester.saksbehandler.idToken
import no.nav.k9.tjenester.saksbehandler.oppgave.OppgaveTjeneste
import java.time.LocalDate

@KtorExperimentalLocationsAPI
fun Route.SaksbehandlerNøkkeltallApis(
    configuration: Configuration,
    requestContextService: RequestContextService,
    oppgaveTjeneste: OppgaveTjeneste
) {
    @Location("/nokkeltall/nye-og-ferdigstilte-oppgaver")
    class getNyeOgFerdigstilteOppgaver

    get { _: getNyeOgFerdigstilteOppgaver ->
        if (configuration.erIkkeLokalt) {
            val idToken = call.idToken()
            withContext(
                requestContextService.getCoroutineContext(
                    context = coroutineContext,
                    idToken = idToken
                )
            ) {
                call.respond(
                    call.respond(oppgaveTjeneste.hentNyeOgFerdigstilteOppgaver())
                )
            }
        } else {
            call.respond(oppgaveTjeneste.hentNyeOgFerdigstilteOppgaver())
        }
    }
}
