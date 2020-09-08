package no.nav.k9.tjenester.avdelingsleder.nokkeltall

import io.ktor.application.call
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.util.KtorExperimentalAPI
import no.nav.k9.domene.modell.BehandlingType
import no.nav.k9.domene.modell.FagsakYtelseType
import no.nav.k9.tjenester.saksbehandler.oppgave.OppgaveTjeneste
import org.koin.ktor.ext.inject
import java.time.LocalDate

@KtorExperimentalAPI
@KtorExperimentalLocationsAPI
fun Route.NokkeltallApis() {
    val nokkeltallTjeneste by inject<NokkeltallTjeneste>()
    val oppgaveTjeneste by inject<OppgaveTjeneste>()
    @Location("/behandlinger-under-arbeid")
    class getAlleOppgaver

    get { _: getAlleOppgaver ->
        call.respond(nokkeltallTjeneste.hentOppgaverUnderArbeid())
    }

    @Location("/beholdning-historikk")
    class getAntallOppgaverPerDato

    get { _: getAntallOppgaverPerDato ->
        call.respond(oppgaveTjeneste.hentBeholdningAvOppgaverPerAntallDager())
    }

    @Location("/ferdigstilte-behandlinger-historikk")
    class getFerdigstilteOppgaver

    get { _: getFerdigstilteOppgaver ->
        call.respond(nokkeltallTjeneste.hentFerdigstilteOppgaver())
    }

}
