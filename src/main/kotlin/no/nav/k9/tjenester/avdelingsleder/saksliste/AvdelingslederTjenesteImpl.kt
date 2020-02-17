//package no.nav.k9.tjenester.avdelingsleder.saksliste
//
//import no.nav.k9.domene.lager.oppgave.*
//import no.nav.k9.domene.repository.OppgaveRepository
//import no.nav.k9.domene.repository.OrganisasjonRepository
//import no.nav.k9.tjenester.avdelingsleder.AvdelingslederTjeneste
//import org.slf4j.LoggerFactory
//import java.time.LocalDate
//
//@ApplicationScoped
//class AvdelingslederTjenesteImpl(
//    val organisasjonRepository: OrganisasjonRepository,
//    val oppgaveRepository: OppgaveRepository) : AvdelingslederTjeneste {
//
//    internal constructor() {
//        // for CDI proxy
//    }
//
//    fun hentOppgaveFiltreringer(avdelingsEnhet: String): List<OppgaveFiltrering> {
//        val avdeling = organisasjonRepository.hentAvdelingFraEnhet(avdelingsEnhet)
//        return oppgaveRepository.hentAlleLister(avdeling.getId())
//    }
//
//    fun hentOppgaveFiltering(oppgaveFiltrering: Long?): OppgaveFiltrering {
//        return oppgaveRepository.hentListe(oppgaveFiltrering)
//    }
//
//    fun lagNyOppgaveFiltrering(avdelingEnhet: String): Long? {
//        val avdeling = organisasjonRepository.hentAvdelingFraEnhet(avdelingEnhet)
//        return oppgaveRepository.lagre(OppgaveFiltrering.nyTomOppgaveFiltrering(avdeling))
//    }
//
//    fun giListeNyttNavn(sakslisteId: Long?, navn: String) {
//        oppgaveRepository.oppdaterNavn(sakslisteId, navn)
//    }
//
//    fun slettOppgaveFiltrering(oppgavefiltreringId: Long?) {
//        log.info("Sletter oppgavefilter " + oppgavefiltreringId!!)
//        oppgaveRepository.slettListe(oppgavefiltreringId)
//    }
//
//    fun settSortering(sakslisteId: Long?, sortering: KøSortering) {
//        oppgaveRepository.settSortering(sakslisteId, sortering.getKode())
//    }
//
//    fun endreFiltreringBehandlingType(oppgavefiltreringId: Long?, behandlingType: BehandlingType, checked: Boolean) {
//        val filtre = oppgaveRepository.hentListe(oppgavefiltreringId)
//        if (checked) {//TODO: De utkommenterte linjene må tilbake når tilbakekreving er klart
//            //if(behandlingType != BehandlingType.TILBAKEBETALING)
//            sjekkSorteringForTilbakekreving(oppgavefiltreringId)
//            oppgaveRepository.lagre(FiltreringBehandlingType(filtre, behandlingType))
//        } else {
//            //if(behandlingType == BehandlingType.TILBAKEBETALING) sjekkSorteringForTilbakekreving(oppgavefiltreringId);
//            oppgaveRepository.slettFiltreringBehandlingType(oppgavefiltreringId, behandlingType)
//        }
//        oppgaveRepository.refresh(filtre)
//    }
//
//    private fun sjekkSorteringForTilbakekreving(oppgavefiltreringId: Long?) {
//        val sortering = oppgaveRepository.hentSorteringForListe(oppgavefiltreringId)
//        if (sortering != null && sortering!!.getFeltkategori() === KøSortering.FK_TILBAKEKREVING) {
//            settSortering(oppgavefiltreringId, KøSortering.BEHANDLINGSFRIST)
//        }
//    }
//
//    fun endreFiltreringYtelseType(oppgavefiltreringId: Long?, fagsakYtelseType: FagsakYtelseType?) {
//        val filtre = oppgaveRepository.hentListe(oppgavefiltreringId)
//        filtre.getFiltreringYtelseTyper()
//            .forEach { ytelseType ->
//                oppgaveRepository.slettFiltreringYtelseType(
//                    oppgavefiltreringId,
//                    ytelseType.getFagsakYtelseType()
//                )
//            }
//        if (fagsakYtelseType != null) {
//            oppgaveRepository.lagre(FiltreringYtelseType(filtre, fagsakYtelseType))
//        }
//        oppgaveRepository.refresh(filtre)
//    }
//
//    fun endreFiltreringAndreKriterierType(
//        oppgavefiltreringId: Long?,
//        andreKriterierType: AndreKriterierType,
//        checked: Boolean,
//        inkluder: Boolean
//    ) {
//        val filtre = oppgaveRepository.hentListe(oppgavefiltreringId)
//        if (checked) {
//            oppgaveRepository.slettFiltreringAndreKriterierType(oppgavefiltreringId, andreKriterierType)
//            oppgaveRepository.lagre(FiltreringAndreKriterierType(filtre, andreKriterierType, inkluder))
//        } else {
//            oppgaveRepository.slettFiltreringAndreKriterierType(oppgavefiltreringId, andreKriterierType)
//        }
//        oppgaveRepository.refresh(filtre)
//    }
//
//    fun leggSaksbehandlerTilListe(oppgaveFiltreringId: Long?, saksbehandlerIdent: String) {
//        val oppgaveListe = oppgaveRepository.hentListe(oppgaveFiltreringId)
//        if (oppgaveListe == null) {
//            log.warn(
//                String.format(
//                    "Fant ikke oppgavefiltreringsliste basert på id %s, saksbehandler %s legges ikke til oppgavefiltrering",
//                    oppgaveFiltreringId,
//                    saksbehandlerIdent
//                )
//            )
//            return
//        }
//        val saksbehandler = organisasjonRepository.hentSaksbehandler(saksbehandlerIdent)
//        oppgaveListe!!.leggTilSaksbehandler(saksbehandler)
//        oppgaveRepository.lagre(oppgaveListe)
//        oppgaveRepository.refresh(saksbehandler)
//    }
//
//    fun fjernSaksbehandlerFraListe(oppgaveFiltreringId: Long?, saksbehandlerIdent: String) {
//        val oppgaveListe = oppgaveRepository.hentListe(oppgaveFiltreringId)
//        if (oppgaveListe == null) {
//            log.warn(
//                String.format(
//                    "Fant ikke oppgavefiltreringsliste basert på id %s, saksbehandler %s fjernes ikke fra oppgavefiltrering",
//                    oppgaveFiltreringId,
//                    saksbehandlerIdent
//                )
//            )
//            return
//        }
//        val saksbehandler = organisasjonRepository.hentSaksbehandler(saksbehandlerIdent)
//        oppgaveListe!!.fjernSaksbehandler(saksbehandler)
//        oppgaveRepository.lagre(oppgaveListe)
//        oppgaveRepository.refresh(saksbehandler)
//    }
//
//    fun hentAvdelinger(): List<Avdeling> {
//        return organisasjonRepository.hentAvdelinger()
//    }
//
//    fun settSorteringTidsintervallDato(oppgaveFiltreringId: Long?, fomDato: LocalDate, tomDato: LocalDate) {
//        oppgaveRepository.settSorteringTidsintervallDato(oppgaveFiltreringId, fomDato, tomDato)
//    }
//
//    fun settSorteringNumeriskIntervall(oppgaveFiltreringId: Long?, fra: Long?, til: Long?) {
//        oppgaveRepository.settSorteringNumeriskIntervall(oppgaveFiltreringId, fra, til)
//    }
//
//    fun settSorteringTidsintervallValg(oppgaveFiltreringId: Long?, erDynamiskPeriode: Boolean) {
//        oppgaveRepository.settSorteringTidsintervallValg(oppgaveFiltreringId, erDynamiskPeriode)
//    }
//
//    companion object {
//        private val log = LoggerFactory.getLogger(AvdelingslederTjenesteImpl::class.java)
//    }
//
//}