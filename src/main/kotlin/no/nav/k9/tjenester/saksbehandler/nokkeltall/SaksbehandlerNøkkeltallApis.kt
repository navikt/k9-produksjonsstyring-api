package no.nav.k9.tjenester.saksbehandler.nokkeltall

import io.ktor.application.call
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.util.KtorExperimentalAPI
import no.nav.k9.domene.modell.BehandlingType
import no.nav.k9.domene.modell.FagsakYtelseType
import no.nav.k9.integrasjon.rest.RequestContextService
import no.nav.k9.tjenester.saksbehandler.oppgave.OppgaveTjeneste
import org.koin.ktor.ext.inject
import java.time.LocalDate

@KtorExperimentalAPI
@KtorExperimentalLocationsAPI
fun Route.SaksbehandlerNøkkeltallApis() {
    val requestContextService by inject<RequestContextService>()
    val oppgaveTjeneste by inject<OppgaveTjeneste>()

    @Location("/nokkeltall/nye-og-ferdigstilte-oppgaver")
    class getNyeOgFerdigstilteOppgaver

    get { _: getNyeOgFerdigstilteOppgaver ->
        requestContextService.withRequestContext(call) {
            call.respond(listOf(NyeOgFerdigstilteOppgaverDto(
                BehandlingType.FORSTEGANGSSOKNAD,
                fagsakYtelseType = FagsakYtelseType.OMSORGSPENGER,
                dato = LocalDate.of(2021, 6, 9),
                antallNye = 36,
                antallFerdigstilte = 5,
                antallFerdigstilteMine = 2
            ), NyeOgFerdigstilteOppgaverDto(
                BehandlingType.FORSTEGANGSSOKNAD,
                fagsakYtelseType = FagsakYtelseType.OMSORGSPENGER,
                dato = LocalDate.of(2021, 6, 8),
                antallNye = 45,
                antallFerdigstilte = 12,
                antallFerdigstilteMine = 3
            ), NyeOgFerdigstilteOppgaverDto(
                BehandlingType.FORSTEGANGSSOKNAD,
                fagsakYtelseType = FagsakYtelseType.PLEIEPENGER_SYKT_BARN,
                dato = LocalDate.of(2021, 6, 9),
                antallNye = 67,
                antallFerdigstilte = 23,
                antallFerdigstilteMine = 9
            ), NyeOgFerdigstilteOppgaverDto(
                BehandlingType.FORSTEGANGSSOKNAD,
                fagsakYtelseType = FagsakYtelseType.PLEIEPENGER_SYKT_BARN,
                dato = LocalDate.of(2021, 6, 8),
                antallNye = 34,
                antallFerdigstilte = 4,
                antallFerdigstilteMine = 1
            )))
        }
    }
}
