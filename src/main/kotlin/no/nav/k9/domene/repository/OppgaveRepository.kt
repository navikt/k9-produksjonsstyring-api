//package no.nav.k9.domene.repository
//
//import no.nav.foreldrepenger.loslager.oppgave.AndreKriterierType
//import no.nav.foreldrepenger.loslager.oppgave.BehandlingType
//import no.nav.foreldrepenger.loslager.oppgave.EventmottakFeillogg
//import no.nav.foreldrepenger.loslager.oppgave.FagsakYtelseType
//import no.nav.foreldrepenger.loslager.oppgave.FiltreringAndreKriterierType
//import no.nav.foreldrepenger.loslager.oppgave.FiltreringBehandlingType
//import no.nav.foreldrepenger.loslager.oppgave.FiltreringYtelseType
//import no.nav.foreldrepenger.loslager.oppgave.KøSortering
//import no.nav.foreldrepenger.loslager.oppgave.Oppgave
//import no.nav.foreldrepenger.loslager.oppgave.OppgaveEgenskap
//import no.nav.foreldrepenger.loslager.oppgave.OppgaveEventLogg
//import no.nav.foreldrepenger.loslager.oppgave.OppgaveFiltrering
//import no.nav.foreldrepenger.loslager.oppgave.Reservasjon
//import no.nav.foreldrepenger.loslager.oppgave.ReservasjonEventLogg
//import no.nav.foreldrepenger.loslager.oppgave.TilbakekrevingOppgave
//import no.nav.foreldrepenger.loslager.organisasjon.Avdeling
//import no.nav.foreldrepenger.loslager.organisasjon.Saksbehandler
//import no.nav.k9.domene.lager.oppgave.Oppgave
//import no.nav.k9.domene.lager.oppgave.Reservasjon
//
//import java.time.LocalDate
//import java.util.UUID
//
//interface OppgaveRepository {
//
//    fun hentOppgaver(oppgavespørringDto: OppgavespørringDto): List<Oppgave>
//
//    fun hentAntallOppgaver(oppgavespørringDto: OppgavespørringDto): Int
//
//    fun hentAntallOppgaverForAvdeling(avdelingsId: Long?): Int
//
//    fun hentReservasjonerTilknyttetAktiveOppgaver(uid: String): List<Reservasjon>
//
//    fun hentOppgaverForSaksnummer(fagsakSaksnummer: Long?): List<Oppgave>
//
//    fun hentAktiveOppgaverForSaksnummer(fagsakSaksnummerListe: Collection<Long>): List<Oppgave>
//
//    fun hentReservasjon(oppgaveId: Long?): Reservasjon
//
//    fun reserverOppgaveFraTidligereReservasjon(oppgaveId: Long?, tidligereReservasjon: Reservasjon)
//
//    fun hentAlleLister(avdelingsId: Long?): List<OppgaveFiltrering>
//
//    fun hentListe(listeId: Long?): OppgaveFiltrering
//
//    fun hentSorteringForListe(listeId: Long?): KøSortering
//
//    fun lagre(oppgave: Reservasjon)
//
//    fun lagre(oppgave: Oppgave)
//
//    fun lagre(egenskaper: TilbakekrevingOppgave)
//
//    fun lagre(oppgaveFiltrering: OppgaveFiltrering): Long?
//
//    fun oppdaterNavn(sakslisteId: Long?, navn: String)
//
//    fun slettListe(listeId: Long?)
//
//    fun settSortering(sakslisteId: Long?, sortering: String)
//
//    fun lagre(filtreringBehandlingType: FiltreringBehandlingType)
//
//    fun lagre(filtreringYtelseType: FiltreringYtelseType)
//
//    fun lagre(filtreringAndreKriterierType: FiltreringAndreKriterierType)
//
//    fun slettFiltreringBehandlingType(sakslisteId: Long?, behandlingType: BehandlingType)
//
//    fun slettFiltreringYtelseType(sakslisteId: Long?, behandlingType: FagsakYtelseType)
//
//    fun slettFiltreringAndreKriterierType(oppgavefiltreringId: Long?, andreKriterierType: AndreKriterierType)
//
//    fun refresh(oppgave: Oppgave)
//
//    fun refresh(oppgaveFiltrering: OppgaveFiltrering)
//
//    fun refresh(avdeling: Avdeling)
//
//    fun refresh(saksbehandler: Saksbehandler)
//
//    fun sjekkOmOppgaverFortsattErTilgjengelige(oppgaveIder: List<Long>): List<Oppgave>
//
//    fun opprettOppgave(oppgave: Oppgave): Oppgave
//
//    fun opprettTilbakekrevingEgenskaper(egenskaper: TilbakekrevingOppgave): TilbakekrevingOppgave
//
//    @Deprecated("Bruk gjenåpneOppgaveForEksternId(Long) i stedet")
//    fun gjenåpneOppgave(behandlingId: Long?): Oppgave
//
//    fun gjenåpneOppgaveForEksternId(eksternId: UUID): Oppgave
//
//
//    @Deprecated("Bruk avsluttOppgaveForEksternId(Long) i stedet")
//    fun avsluttOppgave(behandlingId: Long?)
//
//    fun avsluttOppgaveForEksternId(eksternId: UUID)
//
//    fun hentSisteReserverteOppgaver(uid: String): List<Oppgave>
//
//    fun lagre(oppgaveEgenskap: OppgaveEgenskap)
//
//    fun lagre(eventmottakFeillogg: EventmottakFeillogg)
//
//
//    @Deprecated("Bruk hentEventerForEksternId(Long) i stedet")
//    fun hentEventer(behandlingId: Long?): List<OppgaveEventLogg>
//
//    fun hentEventerForEksternId(eksternId: UUID): List<OppgaveEventLogg>
//
//    fun hentOppgaveEgenskaper(oppgaveId: Long?): List<OppgaveEgenskap>
//
//    fun lagre(oppgaveEventLogg: OppgaveEventLogg)
//
//    fun lagre(oppgaveEventLogg: ReservasjonEventLogg)
//
//    fun settSorteringTidsintervallDato(oppgaveFiltreringId: Long?, fomDato: LocalDate, tomDato: LocalDate)
//
//    fun settSorteringNumeriskIntervall(oppgaveFiltreringId: Long?, fra: Long?, til: Long?)
//
//    fun settSorteringTidsintervallValg(oppgaveFiltreringId: Long?, erDynamiskPeriode: Boolean)
//}
