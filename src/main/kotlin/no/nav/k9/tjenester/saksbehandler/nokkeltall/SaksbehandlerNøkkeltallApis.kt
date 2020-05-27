package no.nav.k9.tjenester.saksbehandler.nokkeltall

import io.ktor.application.call
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import no.nav.k9.tjenester.avdelingsleder.oppgaveko.OppgavekøIdDto
import no.nav.k9.tjenester.saksbehandler.oppgave.OppgaveTjeneste

@KtorExperimentalLocationsAPI
fun Route.SaksbehandlerNøkkeltallApis(
    oppgaveTjeneste: OppgaveTjeneste
) {
    @Location("/nokkeltall/nye-og-ferdigstilte-oppgaver")
    class getNyeOgFerdigstilteOppgaver

    get { _: getNyeOgFerdigstilteOppgaver ->
        val param = call.receive<OppgavekøIdDto>()
        call.respond(oppgaveTjeneste.hentNyeOgFerdigstilteOppgaver(param))
    }
}
