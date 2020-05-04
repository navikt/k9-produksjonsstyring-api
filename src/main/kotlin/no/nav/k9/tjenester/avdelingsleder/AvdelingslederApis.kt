package no.nav.k9.tjenester.avdelingsleder

import io.ktor.application.call
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.locations.post
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.util.KtorExperimentalAPI
import no.nav.k9.tjenester.saksbehandler.oppgave.OppgaveTjeneste
import java.util.*

@KtorExperimentalAPI
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

    @Location("/oppgaver/antall")
    class hentAntallOppgaver()

    get { _: hentAntallOppgaver ->
        val uuid = call.parameters["id"]
        call.respond(oppgaveTjeneste.hentAntallOppgaver(UUID.fromString(uuid)))
    }

    @Location("/saksbehandlere")
    class hentSaksbehandlere

    get { _: hentSaksbehandlere ->
        call.respond(avdelingslederTjeneste.hentSaksbehandlere())
    }

    @Location("/saksbehandlere/sok")
    class søkSaksbehandler

    post { _: søkSaksbehandler ->
        val epost = call.receive<EpostDto>()
        avdelingslederTjeneste.søkSaksbehandler(epost)?.let { call.respond(it) }
    }
}
