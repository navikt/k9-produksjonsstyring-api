package no.nav.k9.tjenester.avdelingsleder.oppgaveko

import io.ktor.application.call
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.locations.post
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import no.nav.k9.tjenester.avdelingsleder.AvdelingslederTjeneste
import no.nav.k9.tjenester.saksbehandler.oppgave.OppgaveTjeneste
import java.util.*

@KtorExperimentalLocationsAPI
fun Route.AvdelingslederOppgavekøApis(
    avdelingslederTjeneste: AvdelingslederTjeneste
) {
    @Location("/")
    class hentAlleOppgaveKøer

    get { _: hentAlleOppgaveKøer ->
        call.respond(avdelingslederTjeneste.hentOppgaveKøer())
    }

    class opprettOppgaveKø

    post { _: opprettOppgaveKø ->
        call.respond(avdelingslederTjeneste.opprettOppgaveKø())
    }

    @Location("/navn")
    class endreOppgavekoNavn

    post { _: endreOppgavekoNavn ->
        val uuid = call.receive<OppgavekøNavnDto>()
        call.respond(avdelingslederTjeneste.endreOppgavekøNavn(uuid))
    }

    @Location("/slett")
    class slettOppgaveKø

    post { _: slettOppgaveKø ->
        val uuid = call.receive<OppgavekøIdDto>()
        call.respond(avdelingslederTjeneste.slettOppgavekø(uuid.id))
    }

    @Location("/behandlingstype")
    class lagreBehandlingstype

    post { _: lagreBehandlingstype ->
        val behandling = call.receive<BehandlingsTypeDto>()
        call.respond(avdelingslederTjeneste.endreBehandlingsType(behandling))
    }

    @Location("/ytelsetype")
    class lagreYtelsestype

    post { _: lagreYtelsestype ->
        val ytelse = call.receive<YtelsesTypeDto>()
        call.respond(avdelingslederTjeneste.endreYtelsesType(ytelse))
    }

}
