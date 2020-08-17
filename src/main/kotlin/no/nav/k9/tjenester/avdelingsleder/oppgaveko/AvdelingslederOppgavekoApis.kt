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
        val uuid = call.receive<IdDto>()
        call.respond(avdelingslederTjeneste.slettOppgavekø(UUID.fromString(uuid.id)))
    }

    @Location("/behandlingstype")
    class lagreBehandlingstype

    post { _: lagreBehandlingstype ->
        val behandling = call.receive<BehandlingsTypeDto>()
        call.respond(avdelingslederTjeneste.endreBehandlingsType(behandling))
    }

    @Location("/skjermet")
    class lagreSkjermet

    post { _: lagreSkjermet ->
        val behandling = call.receive<SkjermetDto>()
        call.respond(avdelingslederTjeneste.endreSkjerming(behandling))
    }

    @Location("/ytelsetype")
    class lagreYtelsestype

    post { _: lagreYtelsestype ->
        val ytelse = call.receive<YtelsesTypeDto>()
        call.respond(avdelingslederTjeneste.endreYtelsesType(ytelse))
    }

    @Location("/andre-kriterier")
    class endreKriterier

    post { _: endreKriterier ->
        val kriterium = call.receive<AndreKriterierDto>()
        call.respond(avdelingslederTjeneste.endreKriterium(kriterium))
    }

    @Location("/sortering")
    class lagreSortering

    post { _: lagreSortering ->
        val sortering = call.receive<KøSorteringDto>()
        call.respond(avdelingslederTjeneste.endreKøSortering(sortering))
    }

    @Location("/sortering-tidsintervall-dato")
    class lagreSorteringType

    post { _: lagreSorteringType ->
        val sortering = call.receive<SorteringDatoDto>()
        call.respond(avdelingslederTjeneste.endreKøSorteringDato(sortering))
    }

    @Location("/saksbehandler")
    class leggFjernSaksbehandlerOppgaveko

    post { _: leggFjernSaksbehandlerOppgaveko ->
        val saksbehandler = call.receive<SaksbehandlerOppgavekoDto>()
        call.respond(avdelingslederTjeneste.leggFjernSaksbehandlerOppgavekø(saksbehandler))
    }
}
