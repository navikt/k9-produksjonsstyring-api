//package no.nav.k9.domene.repository
//
//import no.nav.foreldrepenger.loslager.oppgave.AndreKriterierType
//import no.nav.foreldrepenger.loslager.oppgave.BehandlingType
//import no.nav.foreldrepenger.loslager.oppgave.FagsakYtelseType
//import no.nav.foreldrepenger.loslager.oppgave.FiltreringAndreKriterierType
//import no.nav.foreldrepenger.loslager.oppgave.FiltreringBehandlingType
//import no.nav.foreldrepenger.loslager.oppgave.FiltreringYtelseType
//import no.nav.foreldrepenger.loslager.oppgave.KøSortering
//import no.nav.foreldrepenger.loslager.oppgave.OppgaveFiltrering
//import no.nav.k9.domene.lager.oppgave.BehandlingType
//import no.nav.k9.domene.lager.oppgave.FagsakYtelseType
//import no.nav.k9.domene.lager.oppgave.KøSortering
//
//import java.time.LocalDate
//import java.util.stream.Collectors
//
//class OppgavespørringDto {
//    var sortering: KøSortering? = null
//        private set
//    var id: Long? = null
//        private set
//    var behandlingTyper: List<BehandlingType>? = null
//        private set
//    var ytelseTyper: List<FagsakYtelseType>? = null
//        private set
//    var inkluderAndreKriterierTyper: List<AndreKriterierType>? = null
//        private set
//    var ekskluderAndreKriterierTyper: List<AndreKriterierType>? = null
//        private set
//    var isErDynamiskPeriode: Boolean = false
//        private set
//    var filtrerFomDato: LocalDate? = null
//        private set
//    var filtrerTomDato: LocalDate? = null
//        private set
//    var filtrerFra: Long? = null
//        private set
//    var filtrerTil: Long? = null
//        private set
//    private val filtrerFomDager: Long? = null
//    private val filtrerTomDager: Long? = null
//    var forAvdelingsleder: Boolean = false
//
//    constructor(oppgaveFiltrering: OppgaveFiltrering) {
//        sortering = oppgaveFiltrering.getSortering()
//        id = oppgaveFiltrering.getAvdeling().getId()
//        behandlingTyper = behandlingTypeFra(oppgaveFiltrering)
//        ytelseTyper = ytelseType(oppgaveFiltrering)
//        inkluderAndreKriterierTyper = inkluderAndreKriterierTyperFra(oppgaveFiltrering)
//        ekskluderAndreKriterierTyper = ekskluderAndreKriterierTyperFra(oppgaveFiltrering)
//        isErDynamiskPeriode = oppgaveFiltrering.getErDynamiskPeriode()
//        filtrerFomDato = oppgaveFiltrering.getFomDato()
//        filtrerTomDato = oppgaveFiltrering.getTomDato()
//        filtrerFra = oppgaveFiltrering.getFra()
//        filtrerTil = oppgaveFiltrering.getTil()
//    }
//
//    constructor(
//        id: Long?, sortering: KøSortering, behandlingTyper: List<BehandlingType>,
//        ytelseTyper: List<FagsakYtelseType>, inkluderAndreKriterierTyper: List<AndreKriterierType>,
//        ekskluderAndreKriterierTyper: List<AndreKriterierType>, erDynamiskPeriode: Boolean,
//        filtrerFomDato: LocalDate, filtrerTomDato: LocalDate, filtrerFra: Long?, filtrerTil: Long?
//    ) {
//        this.sortering = sortering
//        this.id = id
//        this.behandlingTyper = behandlingTyper
//        this.ytelseTyper = ytelseTyper
//        this.inkluderAndreKriterierTyper = inkluderAndreKriterierTyper
//        this.ekskluderAndreKriterierTyper = ekskluderAndreKriterierTyper
//        this.isErDynamiskPeriode = erDynamiskPeriode
//        this.filtrerFomDato = filtrerFomDato
//        this.filtrerTomDato = filtrerTomDato
//        this.filtrerFra = filtrerFra
//        this.filtrerTil = filtrerTil
//    }
//
//    private fun ekskluderAndreKriterierTyperFra(oppgaveFiltrering: OppgaveFiltrering): List<AndreKriterierType> {
//        return oppgaveFiltrering.getFiltreringAndreKriterierTyper().stream()
//            .filter(???({ FiltreringAndreKriterierType.isEkskluder() }))
//        .map(???({ FiltreringAndreKriterierType.getAndreKriterierType() }))
//        .collect(Collectors.toList<T>())
//    }
//
//    private fun inkluderAndreKriterierTyperFra(oppgaveFiltrering: OppgaveFiltrering): List<AndreKriterierType> {
//        return oppgaveFiltrering.getFiltreringAndreKriterierTyper().stream()
//            .filter(???({ FiltreringAndreKriterierType.isInkluder() }))
//        .map(???({ FiltreringAndreKriterierType.getAndreKriterierType() }))
//        .collect(Collectors.toList<T>())
//    }
//
//    private fun ytelseType(oppgaveFiltrering: OppgaveFiltrering): List<FagsakYtelseType> {
//        return oppgaveFiltrering.getFiltreringYtelseTyper().stream()
//            .map(???({ FiltreringYtelseType.getFagsakYtelseType() }))
//        .collect(Collectors.toList<T>())
//    }
//
//    private fun behandlingTypeFra(oppgaveFiltrering: OppgaveFiltrering): List<BehandlingType> {
//        return oppgaveFiltrering.getFiltreringBehandlingTyper().stream()
//            .map(???({ FiltreringBehandlingType.getBehandlingType() }))
//        .collect(Collectors.toList<T>())
//    }
//}
