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

        call.respond(
            listOf(
                OppgavekøDto(
                    OppgaveKø(
                        UUID.randomUUID(),
                        "Omsorgspenger",
                        KøSortering.OPPRETT_BEHANDLING,
                        listOf(BehandlingType.FORSTEGANGSSOKNAD),
                        listOf(FagsakYtelseType.PLEIEPENGER_SYKT_BARN),
                        Enhet.NASJONAL,
                        false,
                        LocalDate.of(2020,1,1),
                        LocalDate.of(2020, 8, 1),
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
        LocalDate.of(2020,1,1),
        LocalDate.of(2020, 8, 1),
        listOf(Saksbehandler("435twg", "Saksbehandler Sara"))
    )

    get { _: hentSakslistensSaksbehandlere ->
        call.respond(
            listOf(Saksbehandler("8ewer89uf", "SaksbehandlerEllen"))
        )
    }
}
