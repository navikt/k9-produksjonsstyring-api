package no.nav.k9.tjenester.avdelingsleder.nokkeltall

import io.ktor.application.call
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.util.KtorExperimentalAPI
import no.nav.k9.KoinProfile
import no.nav.k9.domene.modell.BehandlingType
import no.nav.k9.domene.modell.FagsakYtelseType

import no.nav.k9.tjenester.saksbehandler.oppgave.OppgaveTjeneste
import org.koin.ktor.ext.inject
import java.time.LocalDate

@KtorExperimentalAPI
@KtorExperimentalLocationsAPI
fun Route.NokkeltallApis() {
    val nokkeltallTjeneste by inject<NokkeltallTjeneste>()
    val oppgaveTjeneste by inject<OppgaveTjeneste>()
    @Location("/behandlinger-under-arbeid")
    class getAlleOppgaver

    get { _: getAlleOppgaver ->
        call.respond(nokkeltallTjeneste.hentOppgaverUnderArbeid())
    }

    @Location("/beholdning-historikk")
    class getAntallOppgaverPerDato

    get { _: getAntallOppgaverPerDato ->
       // call.respond(oppgaveTjeneste.hentBeholdningAvOppgaverPerAntallDager())

        call.respond(listOf(AlleOppgaverBeholdningHistorikk(
                FagsakYtelseType.OMSORGSPENGER,
                BehandlingType.FORSTEGANGSSOKNAD,
                LocalDate.now(),
                345
        ), AlleOppgaverBeholdningHistorikk(
                FagsakYtelseType.OMSORGSPENGER,
                BehandlingType.REVURDERING,
                LocalDate.now(),
                123

        ),AlleOppgaverBeholdningHistorikk(
                FagsakYtelseType.PLEIEPENGER_SYKT_BARN,
                BehandlingType.REVURDERING,
                LocalDate.now(),
                222

        ), AlleOppgaverBeholdningHistorikk(
                FagsakYtelseType.PLEIEPENGER_SYKT_BARN,
                BehandlingType.FORSTEGANGSSOKNAD,
                LocalDate.now(),
                98

        ), AlleOppgaverBeholdningHistorikk(
                FagsakYtelseType.PLEIEPENGER_SYKT_BARN,
                BehandlingType.KLAGE,
                LocalDate.now(),
                78

        ),AlleOppgaverBeholdningHistorikk(
                FagsakYtelseType.PLEIEPENGER_SYKT_BARN,
                BehandlingType.ANKE,
                LocalDate.now(),
                77

        ), AlleOppgaverBeholdningHistorikk(
                FagsakYtelseType.PLEIEPENGER_SYKT_BARN,
                BehandlingType.INNSYN,
                LocalDate.now(),
                114

        ), AlleOppgaverBeholdningHistorikk(
                FagsakYtelseType.OMSORGSPENGER,
                BehandlingType.FORSTEGANGSSOKNAD,
                LocalDate.now().minusDays(1),
                16
        ), AlleOppgaverBeholdningHistorikk(
                FagsakYtelseType.OMSORGSPENGER,
                BehandlingType.REVURDERING,
                LocalDate.now().minusDays(1),
                89

        ),AlleOppgaverBeholdningHistorikk(
                FagsakYtelseType.PLEIEPENGER_SYKT_BARN,
                BehandlingType.REVURDERING,
                LocalDate.now().minusDays(1),
                34

        ), AlleOppgaverBeholdningHistorikk(
                FagsakYtelseType.PLEIEPENGER_SYKT_BARN,
                BehandlingType.FORSTEGANGSSOKNAD,
                LocalDate.now().minusDays(1),
                100

        ), AlleOppgaverBeholdningHistorikk(
                FagsakYtelseType.PLEIEPENGER_SYKT_BARN,
                BehandlingType.KLAGE,
                LocalDate.now().minusDays(1),
                65

        ),AlleOppgaverBeholdningHistorikk(
                FagsakYtelseType.PLEIEPENGER_SYKT_BARN,
                BehandlingType.ANKE,
                LocalDate.now().minusDays(1),
                115

        ), AlleOppgaverBeholdningHistorikk(
                FagsakYtelseType.PLEIEPENGER_SYKT_BARN,
                BehandlingType.INNSYN,
                LocalDate.now().minusDays(1),
                87

        )))
    }

    @Location("/ferdigstilte-behandlinger-historikk")
    class getFerdigstilteOppgaver

    get { _: getFerdigstilteOppgaver ->
        call.respond(nokkeltallTjeneste.hentFerdigstilteOppgaver())
    }

}
