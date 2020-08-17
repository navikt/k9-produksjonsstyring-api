package no.nav.k9.tjenester.avdelingsleder.nokkeltall

import io.ktor.application.call
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.response.respond
import io.ktor.routing.Route
import no.nav.k9.tjenester.saksbehandler.oppgave.OppgaveTjeneste

@KtorExperimentalLocationsAPI
fun Route.NokkeltallApis(
    nokkeltallTjeneste: NokkeltallTjeneste,
    oppgaveTjeneste: OppgaveTjeneste
) {
    @Location("/behandlinger-under-arbeid")
    class getAlleOppgaver

    get { _: getAlleOppgaver ->
        call.respond(nokkeltallTjeneste.hentOppgaverUnderArbeid())
    }

    @Location("/beholdning-historikk")
    class getAntallOppgaverPerDato

    get { _: getAntallOppgaverPerDato ->
        call.respond(oppgaveTjeneste.hentBeholdningAvOppgaverPerDato())
    }

    @Location("/ferdigstilte-behandlinger-historikk")
    class getFerdigstilteOppgaver

    get { _: getFerdigstilteOppgaver ->
        call.respond(nokkeltallTjeneste.hentFerdigstilteOppgaver())
    }

}
