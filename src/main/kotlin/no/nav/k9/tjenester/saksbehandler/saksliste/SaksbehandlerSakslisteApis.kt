package no.nav.k9.tjenester.saksbehandler.saksliste

import io.ktor.application.call
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.response.respond
import io.ktor.routing.Route
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
        val map = hentOppgaveKøer.map { oppgaveKø ->

            OppgavekøDto(
                id = oppgaveKø.id,
                navn = oppgaveKø.navn,
                behandlingTyper = listOf(BehandlingType.FORSTEGANGSSOKNAD, BehandlingType.REVURDERING),
                fagsakYtelseTyper = listOf(FagsakYtelseType.OMSORGSPENGER, FagsakYtelseType.PLEIEPENGER_SYKT_BARN),
                saksbehandlere = oppgaveKø.saksbehandlere,
                kombinert = oppgaveKø.kombinert,
                registrerPapir = oppgaveKø.registrerPapir,
                selvstendigFrilans = oppgaveKø.selvstendigFrilans,
                søktGradering = oppgaveKø.søktGradering,
                utbetalingTilBruker = oppgaveKø.utbetalingTilBruker,
                tilBeslutter = oppgaveKø.tilBeslutter,
                antallBehandlinger = 1000,
                sistEndret = oppgaveKø.sistEndret,
                sortering = SorteringDto(oppgaveKø.sortering, oppgaveKø.fomDato, oppgaveKø.tomDato, false),
                andreKriterierType = listOf()
            )

        }
        call.respond(map)
    }

    @Location("/saksliste/saksbehandlere")
    class hentSakslistensSaksbehandlere

    get { _: hentSakslistensSaksbehandlere ->
        call.respond(
            listOf(Saksbehandler("8ewer89uf", "SaksbehandlerEllen"))
        )
    }
}
