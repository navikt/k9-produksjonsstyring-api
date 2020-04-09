package no.nav.k9.tjenester.saksbehandler.saksliste

import no.nav.k9.domene.lager.oppgave.KøSortering
import no.nav.k9.domene.modell.*
import java.time.LocalDate
import java.util.*

class SakslisteDto(o: OppgaveKø, antallBehandlinger: Int) {
    val sakslisteId: Int = Random().nextInt(56)
    val navn: String
    var behandlingTyper: List<BehandlingType> = o.filtreringBehandlingTyper
    val sistEndret: LocalDate
    var sortering: SorteringDto = SorteringDto(KøSortering.OPPRETT_BEHANDLING, 8976, 97, LocalDate.now(), LocalDate.now(), true)
    var fagsakYtelseTyper: List<FagsakYtelseType> = o.filtreringYtelseTyper
    var andreKriterier: List<AndreKriterierDto>  = o.filtreringAndreKriterierTyper
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
