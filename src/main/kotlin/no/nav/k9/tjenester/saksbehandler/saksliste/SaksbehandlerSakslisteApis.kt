package no.nav.k9.tjenester.saksbehandler.saksliste

import io.ktor.application.call
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.response.respond
import io.ktor.routing.Route
import no.nav.k9.domene.modell.AndreKriterierType
import no.nav.k9.domene.modell.BehandlingType
import no.nav.k9.domene.modell.FagsakYtelseType
import no.nav.k9.domene.modell.Saksbehandler
import no.nav.k9.tjenester.saksbehandler.oppgave.OppgaveTjeneste

@KtorExperimentalLocationsAPI
fun Route.SaksbehandlerSakslisteApis(oppgaveTjeneste: OppgaveTjeneste) {
    @Location("/saksliste")
    class getSakslister

    get { _: getSakslister ->

        val hentOppgaveKøer = oppgaveTjeneste.hentOppgaveKøer()
        val list = hentOppgaveKøer.map { oppgaveKø ->
                val sortering = SorteringDto(oppgaveKø.sortering, oppgaveKø.fomDato, oppgaveKø.tomDato)

                OppgavekøDto(
                    id = oppgaveKø.id,
                    navn = oppgaveKø.navn,
                    behandlingTyper = oppgaveKø.filtreringBehandlingTyper,
                    fagsakYtelseTyper = oppgaveKø.filtreringYtelseTyper,
                    saksbehandlere = listOf(Saksbehandler("alexaban", "Sara Saksbehandler", "alexaban@nav.no")),
                    antallBehandlinger = 1000,
                    sistEndret = oppgaveKø.sistEndret,
                    sortering = sortering,
                    andreKriterier = oppgaveKø.filtreringAndreKriterierType
                )

        }
        call.respond(list)
    }

    @Location("/saksliste/saksbehandlere")
    class hentSakslistensSaksbehandlere

    get { _: hentSakslistensSaksbehandlere ->
        call.respond(
            listOf(Saksbehandler("8ewer89uf", "SaksbehandlerEllen", "ellen@nav.no"))
        )
    }
}
