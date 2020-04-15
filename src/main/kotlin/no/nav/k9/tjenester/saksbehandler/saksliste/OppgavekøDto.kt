package no.nav.k9.tjenester.saksbehandler.saksliste

import no.nav.k9.domene.lager.oppgave.KøSortering
import no.nav.k9.domene.modell.BehandlingType
import no.nav.k9.domene.modell.FagsakYtelseType
import no.nav.k9.domene.modell.OppgaveKø
import java.time.LocalDate
import java.util.*

class OppgavekøDto(o: OppgaveKø, antallBehandlinger: Int) {
    val oppgavekoId: UUID = o.id
    val navn: String
    var behandlingTyper: List<BehandlingType> = o.filtreringBehandlingTyper
    val sistEndret: LocalDate
    var sortering: SorteringDto =
        SorteringDto(KøSortering.OPPRETT_BEHANDLING, null, null, LocalDate.now(), LocalDate.now(), true)
    var fagsakYtelseTyper: List<FagsakYtelseType> = o.filtreringYtelseTyper
    var andreKriterier: List<AndreKriterierDto> = emptyList()
    var antallBehandlinger = 78



    init {
        navn = o.navn
        sistEndret = LocalDate.now()

//        if (!o.filtreringBehandlingTyper.isEmpty()) {
//            behandlingTyper = o.filtreringBehandlingTyper().stream()
//                .map(Fil::getBehandlingType)
//                .collect(Collectors.toList())
//        }
//        if (!o.getFiltreringYtelseTyper().isEmpty()) {
//            fagsakYtelseTyper = o.getFiltreringYtelseTyper().stream()
//                .map(FiltreringYtelseType::getFagsakYtelseType)
//                .collect(Collectors.toList())
//        }
//        if (!o.getFiltreringAndreKriterierTyper().isEmpty()) {
//            andreKriterier = o.getFiltreringAndreKriterierTyper().stream()
//                .map({ AndreKriterierDto() })
//                .collect(Collectors.toList())
        //}
//        sortering = SorteringDto(
//            o.sortering,
//            o.fra,
//            o.til,
//            o.fomDato,
//            o.tomDato,
//            o.erDynamiskPeriode
//        )
//        saksbehandlerIdenter = o.saksbehandlere.stream()
//            .map(Saksbehandler::saksbehandlerIdent)
//            .collect(Collectors.toList())
//        this.antallBehandlinger = antallBehandlinger
    }
}
