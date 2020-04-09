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

        val of = OppgaveKø("Filtrering 1", KøSortering.OPPRETT_BEHANDLING, listOf(BehandlingType.FORSTEGANGSSOKNAD),
            listOf(FagsakYtelseType.PLEIEPENGER_SYKT_BARN), listOf(AndreKriterierDto(AndreKriterierType.UTBETALING_TIL_BRUKER, true)), Enhet("Enhet", "Enhetsnavn", emptyList(), false),
            12, true, LocalDate.now(), LocalDate.now(), 32, 65, listOf(SaksbehandlerDto("435twg", "Saksbehandler Klara", listOf("Avd"))))

        val of2 = OppgaveKø("Filtrering 2", KøSortering.OPPRETT_BEHANDLING, listOf(BehandlingType.FORSTEGANGSSOKNAD),
            listOf(FagsakYtelseType.PLEIEPENGER_SYKT_BARN), listOf(AndreKriterierDto(AndreKriterierType.UTBETALING_TIL_BRUKER, true)), Enhet("Enhet", "Enhetsnavn", emptyList(), false),
            12, true, LocalDate.now(), LocalDate.now(), 32, 65, listOf(SaksbehandlerDto("3e2r43t","Saksbehandler Gro", listOf("Avdelign"))))

        call.respond(listOf(SakslisteDto(OppgaveKø("Behandlingskø 1", KøSortering.OPPRETT_BEHANDLING, listOf(BehandlingType.FORSTEGANGSSOKNAD),
            listOf(FagsakYtelseType.PLEIEPENGER_SYKT_BARN), listOf(AndreKriterierDto(AndreKriterierType.UTBETALING_TIL_BRUKER, true)), Enhet("Enhet", "Enhetsnavn", listOf(of), false),
            12, true, LocalDate.now(), LocalDate.now(), 32, 65, listOf(SaksbehandlerDto("645fgd","Saksbehandler Klara", listOf("Avdelign")))), 14)))
    }

    @Location("/saksliste/saksbehandlere")
    class hentSakslistensSaksbehandlere

    val of = OppgaveKø("navn", KøSortering.OPPRETT_BEHANDLING, listOf(BehandlingType.FORSTEGANGSSOKNAD),
        listOf(FagsakYtelseType.PLEIEPENGER_SYKT_BARN), listOf(AndreKriterierDto(AndreKriterierType.UTBETALING_TIL_BRUKER, true)), Enhet("Enhet", "Enhetsnavn", emptyList(), false),
        12, true, LocalDate.now(), LocalDate.now(), 32, 65, listOf(SaksbehandlerDto("435twg", "Saksbehandler Sara", listOf("Avd"))))

    get { _: hentSakslistensSaksbehandlere ->
        call.respond(listOf(SaksbehandlerDto("8ewer89uf","SaksbehandlerEllen", listOf("Avdelign")))
          )
    }
}
