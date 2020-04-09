package no.nav.k9.tjenester.saksbehandler.saksliste

import io.ktor.application.call
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.response.respond
import io.ktor.routing.Route
import no.nav.k9.domene.modell.*
import java.time.LocalDate
import java.util.*

@KtorExperimentalLocationsAPI
fun Route.SaksbehandlerSakslisteApis(
) {
    @Location("/saksliste")
    class getSakslister

    get { _: getSakslister ->

        val of = OppgaveKø(
            UUID.randomUUID(), "Filtrering 1", KøSortering.OPPRETT_BEHANDLING, listOf(BehandlingType.FORSTEGANGSSOKNAD),
            listOf(FagsakYtelseType.PLEIEPENGER_SYKT_BARN), Enhet.NASJONAL,
            true, LocalDate.now(), LocalDate.now(), listOf(Saksbehandler("435twg", "Saksbehandler Klara"))
        )

        val of2 = OppgaveKø(
            UUID.randomUUID(), "Filtrering 2", KøSortering.OPPRETT_BEHANDLING, listOf(BehandlingType.FORSTEGANGSSOKNAD),
            listOf(FagsakYtelseType.PLEIEPENGER_SYKT_BARN), Enhet.NASJONAL,
            true, LocalDate.now(), LocalDate.now(), listOf(Saksbehandler("3e2r43t", "Saksbehandler Gro"))
        )

        call.respond(
            listOf(
                SakslisteDto(
                    OppgaveKø(
                        UUID.randomUUID(),
                        "Behandlingskø 1",
                        KøSortering.OPPRETT_BEHANDLING,
                        listOf(BehandlingType.FORSTEGANGSSOKNAD),
                        listOf(FagsakYtelseType.PLEIEPENGER_SYKT_BARN),
                        Enhet.NASJONAL,
                        false,
                        LocalDate.now(),
                        LocalDate.now(),
                        listOf(Saksbehandler("645fgd", "Saksbehandler Klara"))
                    ), 14
                )
            )
        )
    }

    @Location("/saksliste/saksbehandlere")
    class hentSakslistensSaksbehandlere

    val of = OppgaveKø(
        UUID.randomUUID(),
        "navn",
        KøSortering.OPPRETT_BEHANDLING,
        listOf(BehandlingType.FORSTEGANGSSOKNAD),
        listOf(FagsakYtelseType.PLEIEPENGER_SYKT_BARN),
        Enhet.NASJONAL,
        false,
        LocalDate.now(),
        LocalDate.now(),
        listOf(Saksbehandler("435twg", "Saksbehandler Sara"))
    )

    get { _: hentSakslistensSaksbehandlere ->
        call.respond(
            listOf(Saksbehandler("8ewer89uf", "SaksbehandlerEllen"))
        )
    }
}
