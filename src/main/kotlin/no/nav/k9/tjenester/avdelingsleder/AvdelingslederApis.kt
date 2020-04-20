package no.nav.k9.tjenester.avdelingsleder

import io.ktor.application.call
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.locations.post
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import no.nav.k9.domene.modell.*
import no.nav.k9.tjenester.avdelingsleder.oppgaveko.OppgavekøIdDto
import no.nav.k9.tjenester.saksbehandler.oppgave.OppgaveTjeneste
import java.time.LocalDate
import java.util.*

@KtorExperimentalLocationsAPI
fun Route.AvdelingslederApis(
    oppgaveTjeneste: OppgaveTjeneste,
    avdelingslederTjeneste: AvdelingslederTjeneste
) {

    @Location("/oppgaver/antall-totalt")
    class hentAntallOppgaverForAvdeling

    get { _: hentAntallOppgaverForAvdeling ->
        val antall = oppgaveTjeneste.hentAntallOppgaverTotalt()
        call.respond(antall)
    }


}
