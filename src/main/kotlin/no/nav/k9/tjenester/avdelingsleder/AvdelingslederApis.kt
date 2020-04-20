package no.nav.k9.tjenester.avdelingsleder

import io.ktor.application.call
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.response.respond
import io.ktor.routing.Route
import no.nav.k9.domene.modell.*
import no.nav.k9.tjenester.saksbehandler.oppgave.OppgaveTjeneste
import java.time.LocalDate
import java.util.*

@KtorExperimentalLocationsAPI
fun Route.AvdelingslederApis(
    oppgaveTjeneste: OppgaveTjeneste,
    avdelingslederTjeneste: AvdelingslederTjeneste
) {
    @Location("/avdelinger")
    class getAvdelinger

    get { _: getAvdelinger ->
        call.respond(listOf(Avdeling(33, "Enhet", "NASJONAL", false)))
    }

    @Location("/oppgaver/avdelingantall")
    class hentAntallOppgaverForAvdeling

    get { _: hentAntallOppgaverForAvdeling ->
        val antall = oppgaveTjeneste.hentAntallOppgaverForAvdeling()
        call.respond(antall)
    }

    @Location("/oppgavekoer")
    class hentOppgaveKøerForAvdelingsleder

    get { _: hentOppgaveKøerForAvdelingsleder ->
        avdelingslederTjeneste.opprettOppgaveKø(OppgaveKø(
            UUID.randomUUID(),
            "Pleiepenger",
            LocalDate.now(),
            KøSortering.BEHANDLINGSFRIST,
            listOf(BehandlingType.FORSTEGANGSSOKNAD),
            listOf(FagsakYtelseType.PLEIEPENGER_SYKT_BARN),
            Enhet.NASJONAL,
            false,
            LocalDate.of(2020, 1, 1),
            LocalDate.of(2020, 8, 23),
            listOf(Saksbehandler("9302r84", "Saksbehandler Eva"), Saksbehandler("r98437t", "Guro Saksbehandler")),
            utbetalingTilBruker = false,
            søktGradering = false,
            selvstendigFrilans = false,
            registrerPapir = false,
            kombinert = false,
            tilBeslutter = false
        ))
        call.respond(avdelingslederTjeneste.hentOppgaveKøer())
    }
}
