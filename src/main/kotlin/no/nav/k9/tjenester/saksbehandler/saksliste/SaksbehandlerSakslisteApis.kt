package no.nav.k9.tjenester.saksbehandler.saksliste

import io.ktor.application.call
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.response.respond
import io.ktor.routing.Route
import no.nav.k9.domene.modell.*
import java.time.LocalDate

@KtorExperimentalLocationsAPI
fun Route.SaksbehandlerSakslisteApis(
) {
    @Location("/saksliste")
    class getSakslister

    get { _: getSakslister ->

        val of = OppgaveFiltrering("Filtrering 1", KøSortering.OPPRETT_BEHANDLING, listOf(BehandlingType.FORSTEGANGSSOKNAD),
            listOf(FagsakYtelseType.PLEIEPENGER_SYKT_BARN), listOf(AndreKriterierDto(AndreKriterierType.UTBETALING_TIL_BRUKER, true)), Enhet("Enhet", "Enhetsnavn", emptyList(), false),
            12, true, LocalDate.now(), LocalDate.now(), 32, 65, listOf(Saksbehandler("gkqwreilw", 546, emptyList(), emptyList())))

        val of2 = OppgaveFiltrering("Filtrering 2", KøSortering.OPPRETT_BEHANDLING, listOf(BehandlingType.FORSTEGANGSSOKNAD),
            listOf(FagsakYtelseType.PLEIEPENGER_SYKT_BARN), listOf(AndreKriterierDto(AndreKriterierType.UTBETALING_TIL_BRUKER, true)), Enhet("Enhet", "Enhetsnavn", emptyList(), false),
            12, true, LocalDate.now(), LocalDate.now(), 32, 65, listOf(Saksbehandler("gkqwreilw", 546, emptyList(), emptyList())))

        call.respond(listOf(SakslisteDto(OppgaveFiltrering("Behandlingskø 1", KøSortering.OPPRETT_BEHANDLING, listOf(BehandlingType.FORSTEGANGSSOKNAD),
            listOf(FagsakYtelseType.PLEIEPENGER_SYKT_BARN), listOf(AndreKriterierDto(AndreKriterierType.UTBETALING_TIL_BRUKER, true)), Enhet("Enhet", "Enhetsnavn", listOf(of), false),
            12, true, LocalDate.now(), LocalDate.now(), 32, 65, listOf(Saksbehandler("gkqwreilw", 546, emptyList(), emptyList()))), 13),
            SakslisteDto(OppgaveFiltrering("Behandlingskø 2", KøSortering.OPPRETT_BEHANDLING, listOf(BehandlingType.FORSTEGANGSSOKNAD),
                listOf(FagsakYtelseType.PLEIEPENGER_SYKT_BARN), listOf(AndreKriterierDto(AndreKriterierType.UTBETALING_TIL_BRUKER, true)), Enhet("Enhet", "Enhetsnavn", listOf(of2), false),
                12, true, LocalDate.now(), LocalDate.now(), 32, 65, listOf(Saksbehandler("gkqwreilw", 546, emptyList(), emptyList()))), 13)))
    }

    @Location("/saksliste/saksbehandlere")
    class hentSakslistensSaksbehandlere

    val of = OppgaveFiltrering("navn", KøSortering.OPPRETT_BEHANDLING, listOf(BehandlingType.FORSTEGANGSSOKNAD),
        listOf(FagsakYtelseType.PLEIEPENGER_SYKT_BARN), listOf(AndreKriterierDto(AndreKriterierType.UTBETALING_TIL_BRUKER, true)), Enhet("Enhet", "Enhetsnavn", emptyList(), false),
        12, true, LocalDate.now(), LocalDate.now(), 32, 65, listOf(Saksbehandler("gkqwreilw", 546, emptyList(), emptyList())))

    get { _: hentSakslistensSaksbehandlere ->
        call.respond(listOf(Saksbehandler("Klara Saksbehandler", 13, listOf(Enhet("wegqh", "wieuegru", listOf(of), false)),
            emptyList())))
    }
}