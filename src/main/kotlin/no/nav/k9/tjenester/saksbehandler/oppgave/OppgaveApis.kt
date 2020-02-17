package no.nav.k9.tjenester.saksbehandler.oppgave

import io.ktor.application.call
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.response.respond
import io.ktor.routing.Route
import no.nav.k9.tjenester.avdelingsleder.InnloggetNavAnsatt

@KtorExperimentalLocationsAPI
fun Route.OppgaverApis(
) {
    @Location("/saksbehandler/oppgaver")
    class getSaksbehandlerOppgaver

}