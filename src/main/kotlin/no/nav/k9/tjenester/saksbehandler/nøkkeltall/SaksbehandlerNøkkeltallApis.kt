package no.nav.k9.tjenester.saksbehandler.nøkkeltall

import io.ktor.application.call
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.response.respond
import io.ktor.routing.Route
import no.nav.k9.domene.modell.BehandlingType
import java.time.LocalDate

@KtorExperimentalLocationsAPI
fun Route.SaksbehandlerNøkkeltallApis(
) {
    @Location("/nokkeltall/nye-og-ferdigstilte-oppgaver")
    class getNyeOgFerdigstilteOppgaver

    get { _: getNyeOgFerdigstilteOppgaver ->
        val queryParameter = call.request.queryParameters["sakslisteId"]

        call.respond(listOf(NyeOgFerdigstilteOppgaverDto(BehandlingType.FORSTEGANGSSOKNAD, 65, 98, LocalDate.now()),
            NyeOgFerdigstilteOppgaverDto(BehandlingType.FORSTEGANGSSOKNAD, 13, 9, LocalDate.now())))

    }
}
