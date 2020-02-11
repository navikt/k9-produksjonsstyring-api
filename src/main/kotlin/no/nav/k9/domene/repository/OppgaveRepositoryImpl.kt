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
//import no.nav.foreldrepenger.loslager.oppgave.OppgaveFiltreringOppdaterer
//import no.nav.foreldrepenger.loslager.oppgave.Reservasjon
//import no.nav.foreldrepenger.loslager.oppgave.ReservasjonEventLogg
//import no.nav.foreldrepenger.loslager.oppgave.TilbakekrevingOppgave
//import no.nav.foreldrepenger.loslager.organisasjon.Avdeling
//import no.nav.foreldrepenger.loslager.organisasjon.Saksbehandler
//import no.nav.vedtak.felles.jpa.VLPersistenceUnit
//import no.nav.vedtak.sikkerhet.context.SubjectHandler
//import org.hibernate.Criteria
//import org.hibernate.query.criteria.HibernateCriteriaBuilder
//
//import javax.enterprise.context.ApplicationScoped
//import javax.inject.Inject
//import javax.persistence.EntityManager
//import javax.persistence.TypedQuery
//import javax.persistence.criteria.CriteriaBuilder
//import javax.persistence.criteria.CriteriaQuery
//import javax.persistence.criteria.Root
//import java.math.BigDecimal
//import java.time.LocalDate
//import java.time.LocalDateTime
//import java.time.LocalTime
//import java.util.ArrayList
//import java.util.Comparator
//import java.util.UUID
//import java.util.stream.Collectors
//
//import no.nav.foreldrepenger.loslager.BaseEntitet.BRUKERNAVN_NÅR_SIKKERHETSKONTEKST_IKKE_FINNES
//import no.nav.foreldrepenger.loslager.oppgave.KøSortering.FT_DATO
//import no.nav.foreldrepenger.loslager.oppgave.KøSortering.FT_HELTALL
//
//@ApplicationScoped
//class OppgaveRepositoryImpl : OppgaveRepository {
//
//    internal val entityManager: EntityManager
//
//    @Inject
//    constructor(@VLPersistenceUnit entityManager: EntityManager) {
//        this.entityManager = entityManager
//    }
//
//    internal constructor() {}
//
//    override fun hentAntallOppgaver(oppgavespørringDto: OppgavespørringDto): Int {
//        var selection = COUNT_FRA_OPPGAVE
//        if (oppgavespørringDto.sortering != null) {
//            when (oppgavespørringDto.sortering!!.getFeltkategori()) {
//                KøSortering.FK_TILBAKEKREVING -> selection = COUNT_FRA_TILBAKEKREVING_OPPGAVE
//                KøSortering.FK_UNIVERSAL -> selection = COUNT_FRA_OPPGAVE
//            }
//        }
//        val oppgaveTypedQuery = lagOppgavespørring(selection, Long::class.java, oppgavespørringDto)
//        return oppgaveTypedQuery.getSingleResult().intValue()
//    }
//
//    override fun hentAntallOppgaverForAvdeling(avdelingsId: Long?): Int {
//        val oppgavespørringDto = OppgavespørringDto(
//            avdelingsId,
//            KøSortering.BEHANDLINGSFRIST,
//            ArrayList(),
//            ArrayList(),
//            ArrayList(),
//            ArrayList(),
//            false,
//            null,
//            null,
//            null,
//            null
//        )
//        val oppgaveTypedQuery = lagOppgavespørring(COUNT_FRA_OPPGAVE, Long::class.java, oppgavespørringDto)
//        return oppgaveTypedQuery.getSingleResult().intValue()
//    }
//
//    override fun hentOppgaver(oppgavespørringDto: OppgavespørringDto): List<Oppgave> {
//        var selection = SELECT_FRA_OPPGAVE
//        if (oppgavespørringDto.sortering != null) {
//            when (oppgavespørringDto.sortering!!.getFeltkategori()) {
//                KøSortering.FK_TILBAKEKREVING -> selection = SELECT_FRA_TILBAKEKREVING_OPPGAVE
//                KøSortering.FK_UNIVERSAL -> selection = SELECT_FRA_OPPGAVE
//            }
//        }
//        val oppgaveTypedQuery = lagOppgavespørring(selection, Oppgave::class.java, oppgavespørringDto)
//        return oppgaveTypedQuery.getResultList()
//    }
//
//    //    private <T> CriteriaQuery<T> lagOppgaveSpørring(Class<T> cls, OppgavespørringDto queryDto) {
//    //        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
//    //        CriteriaQuery<T> cq = builder.createQuery(cls);
//    //        Root<T> entityRoot = cq.from(cls);
//    //        cq.select(entityRoot);
//    //    }
//
//    private fun <T> lagOppgavespørring(
//        selection: String,
//        oppgaveClass: Class<T>,
//        queryDto: OppgavespørringDto
//    ): TypedQuery<T> {
//        val filtrerBehandlingType =
//            if (queryDto.behandlingTyper!!.isEmpty()) "" else " o.behandlingType in :behtyper AND "
//        val filtrerYtelseType =
//            if (queryDto.ytelseTyper!!.isEmpty()) "" else " o.fagsakYtelseType in :fagsakYtelseType AND "
//
//        val ekskluderInkluderAndreKriterier = StringBuilder()
//        for (kriterie in queryDto.inkluderAndreKriterierTyper!!) {
//            ekskluderInkluderAndreKriterier.append("EXISTS ( SELECT  1 FROM OppgaveEgenskap oe WHERE o = oe.oppgave AND oe.aktiv = true AND oe.andreKriterierType = '" + kriterie.getKode() + "' ) AND ")
//        }
//        for (kriterie in queryDto.ekskluderAndreKriterierTyper!!) {
//            ekskluderInkluderAndreKriterier.append("NOT EXISTS (select 1 from OppgaveEgenskap oen WHERE o = oen.oppgave AND oen.aktiv = true AND oen.andreKriterierType = '")
//                .append(kriterie.getKode()).append("') AND ")
//        }
//
//        val query = entityManager.createQuery(
//                selection + //$NON-NLS-1$ // NOSONAR
//
//                        "INNER JOIN avdeling a ON a.avdelingEnhet = o.behandlendeEnhet " +
//                        "WHERE " +
//                        filtrerBehandlingType +
//                        filtrerYtelseType +
//                        ekskluderInkluderAndreKriterier +
//                        "NOT EXISTS (select r from Reservasjon r where r.oppgave = o and r.reservertTil > :naa) " +
//                        tilBeslutter(queryDto) +
//                        "AND a.id = :enhet " +
//                        "AND o.aktiv = true " + sortering(queryDto), oppgaveClass
//            )
//            .setParameter("naa", LocalDateTime.now())
//            .setParameter("enhet", queryDto.id)
//
//        if (!queryDto.forAvdelingsleder) {
//            query.setParameter("tilbeslutter", AndreKriterierType.TIL_BESLUTTER)
//                .setParameter("uid", finnBrukernavn())
//        }
//        if (!queryDto.behandlingTyper!!.isEmpty()) {
//            query.setParameter("behtyper", queryDto.behandlingTyper)
//        }
//        if (!queryDto.ytelseTyper!!.isEmpty()) {
//            query.setParameter("fagsakYtelseType", queryDto.ytelseTyper)
//        }
//        if (queryDto.sortering != null) {
//            if (FT_HELTALL.equalsIgnoreCase(queryDto.sortering!!.getFelttype())) {
//                if (queryDto.filtrerFra != null) {
//                    query.setParameter("filterFra", BigDecimal.valueOf(queryDto.filtrerFra!!))
//                }
//                if (queryDto.filtrerTil != null) {
//                    query.setParameter("filterTil", BigDecimal.valueOf(queryDto.filtrerTil!!))
//                }
//            } else if (FT_DATO.equalsIgnoreCase(queryDto.sortering!!.getFelttype())) {
//                if (queryDto.filtrerFra != null) {
//                    query.setParameter(
//                        "filterFomDager",
//                        if (KøSortering.FORSTE_STONADSDAG.equals(queryDto.sortering)) LocalDate.now()
//                            .plusDays(queryDto.filtrerFra!!) else LocalDateTime.now().plusDays(queryDto.filtrerFra!!)
//                            .with(LocalTime.MIN)
//                    )
//                }
//                if (queryDto.filtrerTil != null) {
//                    query.setParameter(
//                        "filterTomDager",
//                        if (KøSortering.FORSTE_STONADSDAG.equals(queryDto.sortering)) LocalDate.now()
//                            .plusDays(queryDto.filtrerTil!!) else LocalDateTime.now().plusDays(queryDto.filtrerTil!!)
//                            .with(LocalTime.MAX)
//                    )
//                }
//                if (queryDto.filtrerFomDato != null) {
//                    query.setParameter(
//                        "filterFomDato",
//                        if (KøSortering.FORSTE_STONADSDAG
//                                .equals(queryDto.sortering)
//                        ) queryDto.filtrerFomDato else queryDto.filtrerFomDato!!.atTime(LocalTime.MIN)
//                    )
//                }
//                if (queryDto.filtrerTomDato != null) {
//                    query.setParameter(
//                        "filterTomDato",
//                        if (KøSortering.FORSTE_STONADSDAG
//                                .equals(queryDto.sortering)
//                        ) queryDto.filtrerTomDato else queryDto.filtrerTomDato!!.atTime(LocalTime.MAX)
//                    )
//                }
//            }
//        }
//
//        return query
//    }
//
//    private fun tilBeslutter(dto: OppgavespørringDto): String {
//        return if (dto.forAvdelingsleder)
//            ""
//        else
//            "AND NOT EXISTS (select oetilbesl.oppgave from OppgaveEgenskap oetilbesl " +
//                    "where oetilbesl.oppgave = o AND oetilbesl.aktiv = true AND oetilbesl.andreKriterierType = :tilbeslutter " +
//                    "AND upper(oetilbesl.sisteSaksbehandlerForTotrinn) = upper( :uid ) ) "
//    }
//
//    private fun sortering(oppgavespørringDto: OppgavespørringDto): String {
//        val sortering = oppgavespørringDto.sortering
//        return if (KøSortering.BEHANDLINGSFRIST.equals(sortering)) {
//            if (oppgavespørringDto.isErDynamiskPeriode)
//                filtrerDynamisk(BEHANDLINGSFRIST, oppgavespørringDto.filtrerFra, oppgavespørringDto.filtrerTil)
//            else
//                filtrerStatisk(BEHANDLINGSFRIST, oppgavespørringDto.filtrerFomDato, oppgavespørringDto.filtrerTomDato)
//        } else if (KøSortering.OPPRETT_BEHANDLING.equals(sortering)) {
//            if (oppgavespørringDto.isErDynamiskPeriode)
//                filtrerDynamisk(BEHANDLINGOPPRETTET, oppgavespørringDto.filtrerFra, oppgavespørringDto.filtrerTil)
//            else
//                filtrerStatisk(
//                    BEHANDLINGOPPRETTET,
//                    oppgavespørringDto.filtrerFomDato,
//                    oppgavespørringDto.filtrerTomDato
//                )
//        } else if (KøSortering.FORSTE_STONADSDAG.equals(sortering)) {
//            if (oppgavespørringDto.isErDynamiskPeriode)
//                filtrerDynamisk(FORSTE_STONADSDAG, oppgavespørringDto.filtrerFra, oppgavespørringDto.filtrerTil)
//            else
//                filtrerStatisk(FORSTE_STONADSDAG, oppgavespørringDto.filtrerFomDato, oppgavespørringDto.filtrerTomDato)
//        } else if (KøSortering.BELOP.equals(sortering)) {
//            filtrerNumerisk(BELOP, oppgavespørringDto.filtrerFra, oppgavespørringDto.filtrerTil)
//        } else if (KøSortering.FEILUTBETALINGSTART.equals(sortering)) {
//            if (oppgavespørringDto.isErDynamiskPeriode)
//                filtrerDynamisk(FEILUTBETALINGSTART, oppgavespørringDto.filtrerFra, oppgavespørringDto.filtrerTil)
//            else
//                filtrerStatisk(
//                    FEILUTBETALINGSTART,
//                    oppgavespørringDto.filtrerFomDato,
//                    oppgavespørringDto.filtrerTomDato
//                )
//        } else {
//            SORTERING + BEHANDLINGOPPRETTET
//        }
//    }
//
//    private fun filtrerNumerisk(sortering: String, fra: Long?, til: Long?): String {
//        var numeriskFiltrering = ""
//        if (fra != null && til != null) {
//            numeriskFiltrering = "AND $sortering >= :filterFra AND $sortering <= :filterTil "
//        } else if (fra != null) {
//            numeriskFiltrering = "AND $sortering >= :filterFra "
//        } else if (til != null) {
//            numeriskFiltrering = "AND $sortering <= :filterTil "
//        }
//        return numeriskFiltrering + SORTERING + sortering
//    }
//
//    private fun filtrerDynamisk(sortering: String, fomDager: Long?, tomDager: Long?): String {
//        var datoFiltrering = ""
//        if (fomDager != null && tomDager != null) {
//            datoFiltrering = "AND $sortering > :filterFomDager AND $sortering < :filterTomDager "
//        } else if (fomDager != null) {
//            datoFiltrering = "AND $sortering > :filterFomDager "
//        } else if (tomDager != null) {
//            datoFiltrering = "AND $sortering < :filterTomDager "
//        }
//        return datoFiltrering + SORTERING + sortering
//    }
//
//    private fun filtrerStatisk(sortering: String, fomDato: LocalDate?, tomDato: LocalDate?): String {
//        var datoFiltrering = ""
//        if (fomDato != null && tomDato != null) {
//            datoFiltrering = "AND $sortering > :filterFomDato AND $sortering < :filterTomDato "
//        } else if (fomDato != null) {
//            datoFiltrering = "AND $sortering > :filterFomDato "
//        } else if (tomDato != null) {
//            datoFiltrering = "AND $sortering < :filterTomDato "
//        }
//        return datoFiltrering + SORTERING + sortering
//    }
//
//    override fun hentReservasjonerTilknyttetAktiveOppgaver(uid: String): List<Reservasjon> {
//        val oppgaveTypedQuery = entityManager.createQuery(
//                "Select r from Reservasjon r " +
//                        "INNER JOIN Oppgave o ON r.oppgave = o " +
//                        "WHERE r.reservertTil > :naa AND upper(r.reservertAv) = upper( :uid ) AND o.aktiv = true",
//                Reservasjon::class.java
//            ) //$NON-NLS-1$
//            .setParameter("naa", LocalDateTime.now())
//            .setParameter("uid", uid)
//        return oppgaveTypedQuery.getResultList()
//    }
//
//    override fun hentOppgaverForSaksnummer(fagsakSaksnummer: Long?): List<Oppgave> {
//        return entityManager.createQuery(
//                SELECT_FRA_OPPGAVE +
//                        "WHERE o.fagsakSaksnummer = :fagsakSaksnummer " +
//                        "ORDER BY o.id desc ", Oppgave::class.java
//            )
//            .setParameter("fagsakSaksnummer", fagsakSaksnummer)
//            .getResultList()
//    }
//
//    override fun hentAktiveOppgaverForSaksnummer(fagsakSaksnummerListe: Collection<Long>): List<Oppgave> {
//        return entityManager.createQuery(
//                SELECT_FRA_OPPGAVE +
//                        "WHERE o.fagsakSaksnummer in :fagsakSaksnummerListe " +
//                        "AND o.aktiv = true " +
//                        "ORDER BY o.fagsakSaksnummer desc ", Oppgave::class.java
//            )
//            .setParameter("fagsakSaksnummerListe", fagsakSaksnummerListe)
//            .getResultList()
//    }
//
//    override fun hentReservasjon(oppgaveId: Long?): Reservasjon {
//        val oppgaveTypedQuery =
//            entityManager.createQuery("from Reservasjon r WHERE r.oppgave.id = :id ", Reservasjon::class.java)
//                .setParameter("id", oppgaveId)//$NON-NLS-1$
//        val resultList = oppgaveTypedQuery.getResultList()
//        return if (resultList.isEmpty()) {
//            Reservasjon(entityManager.find(Oppgave::class.java, oppgaveId))
//        } else oppgaveTypedQuery.getResultList().get(0)
//    }
//
//    override fun reserverOppgaveFraTidligereReservasjon(oppgaveId: Long?, tidligereReservasjon: Reservasjon) {
//        val reservasjon = hentReservasjon(oppgaveId)
//        reservasjon.reserverOppgaveFraTidligereReservasjon(tidligereReservasjon)
//        lagre(reservasjon)
//        refresh(reservasjon.getOppgave())
//        lagre(ReservasjonEventLogg(reservasjon))
//    }
//
//    override fun hentAlleLister(avdelingsId: Long?): List<OppgaveFiltrering> {
//        val listeTypedQuery = entityManager
//            .createQuery(
//                "FROM OppgaveFiltrering l WHERE l.avdeling.id = :id $OPPGAVEFILTRERING_SORTERING_NAVN",
//                OppgaveFiltrering::class.java
//            )
//            .setParameter("id", avdelingsId)//$NON-NLS-1$
//        return listeTypedQuery.getResultList()
//    }
//
//    override fun hentListe(listeId: Long?): OppgaveFiltrering {
//        val listeTypedQuery = entityManager
//            .createQuery(
//                "FROM OppgaveFiltrering l WHERE l.id = :id $OPPGAVEFILTRERING_SORTERING_NAVN",
//                OppgaveFiltrering::class.java
//            )
//            .setParameter("id", listeId)
//        return listeTypedQuery.getResultStream().findFirst().orElse(null)
//    }
//
//    override fun hentSorteringForListe(listeId: Long?): KøSortering {
//        val listeTypedQuery = entityManager
//            .createQuery("SELECT l.sortering FROM OppgaveFiltrering l WHERE l.id = :id ", KøSortering::class.java)
//            .setParameter("id", listeId)
//        return listeTypedQuery.getResultStream().findFirst().orElse(null)
//    }
//
//
//    override fun lagre(reservasjon: Reservasjon) {
//        internLagre(reservasjon)
//    }
//
//    override fun lagre(oppgave: Oppgave) {
//        internLagre(oppgave)
//    }
//
//    override fun lagre(egenskaper: TilbakekrevingOppgave) {
//        internLagre(egenskaper)
//    }
//
//    override fun lagre(filtreringBehandlingType: FiltreringBehandlingType) {
//        internLagre(filtreringBehandlingType)
//    }
//
//    override fun lagre(filtreringYtelseType: FiltreringYtelseType) {
//        internLagre(filtreringYtelseType)
//    }
//
//    override fun lagre(filtreringAndreKriterierType: FiltreringAndreKriterierType) {
//        internLagre(filtreringAndreKriterierType)
//    }
//
//    override fun lagre(oppgaveFiltrering: OppgaveFiltrering): Long? {
//        internLagre(oppgaveFiltrering)
//        return oppgaveFiltrering.getId()
//    }
//
//    override fun oppdaterNavn(sakslisteId: Long?, navn: String) {
//        entityManager.persist(
//            entityManager.find(OppgaveFiltreringOppdaterer::class.java, sakslisteId)
//                .endreNavn(navn)
//        )
//        entityManager.flush()
//    }
//
//    override fun slettListe(listeId: Long?) {
//        entityManager.remove(entityManager.find(OppgaveFiltrering::class.java, listeId))
//        entityManager.flush()
//    }
//
//    override fun slettFiltreringBehandlingType(sakslisteId: Long?, behandlingType: BehandlingType) {
//        entityManager.createNativeQuery("DELETE FROM FILTRERING_BEHANDLING_TYPE f " + "WHERE f.OPPGAVE_FILTRERING_ID = :oppgaveFiltreringId and f.behandling_type = :behandlingType")
//            .setParameter("oppgaveFiltreringId", sakslisteId)//$NON-NLS-1$ // NOSONAR
//            .setParameter("behandlingType", behandlingType.getKode())
//            .executeUpdate()
//    }
//
//    override fun slettFiltreringYtelseType(sakslisteId: Long?, fagsakYtelseType: FagsakYtelseType) {
//        entityManager.createNativeQuery("DELETE FROM FILTRERING_YTELSE_TYPE f " + "WHERE f.OPPGAVE_FILTRERING_ID = :oppgaveFiltreringId and f.FAGSAK_YTELSE_TYPE = :fagsakYtelseType")
//            .setParameter("oppgaveFiltreringId", sakslisteId)//$NON-NLS-1$ // NOSONAR
//            .setParameter("fagsakYtelseType", fagsakYtelseType.getKode())
//            .executeUpdate()
//    }
//
//    override fun slettFiltreringAndreKriterierType(oppgavefiltreringId: Long?, andreKriterierType: AndreKriterierType) {
//        entityManager.createNativeQuery("DELETE FROM FILTRERING_ANDRE_KRITERIER f " + "WHERE f.OPPGAVE_FILTRERING_ID = :oppgaveFiltreringId and f.ANDRE_KRITERIER_TYPE = :andreKriterierType")
//            .setParameter("oppgaveFiltreringId", oppgavefiltreringId)//$NON-NLS-1$ // NOSONAR
//            .setParameter("andreKriterierType", andreKriterierType.getKode())
//            .executeUpdate()
//    }
//
//    override fun refresh(oppgave: Oppgave) {
//        entityManager.refresh(oppgave)
//    }
//
//    override fun refresh(oppgaveFiltrering: OppgaveFiltrering) {
//        entityManager.refresh(oppgaveFiltrering)
//    }
//
//    override fun refresh(avdeling: Avdeling) {
//        entityManager.refresh(avdeling)
//    }
//
//    override fun refresh(saksbehandler: Saksbehandler) {
//        entityManager.refresh(saksbehandler)
//    }
//
//    override fun sjekkOmOppgaverFortsattErTilgjengelige(oppgaveIder: List<Long>): List<Oppgave> {
//        return entityManager.createQuery(
//                SELECT_FRA_OPPGAVE +
//                        " INNER JOIN avdeling a ON a.avdelingEnhet = o.behandlendeEnhet WHERE " +
//                        "NOT EXISTS (select r from Reservasjon r where r.oppgave = o and r.reservertTil > :naa) " +
//                        "AND o.id IN ( :oppgaveId ) " +
//                        "AND o.aktiv = true", Oppgave::class.java
//            ) //$NON-NLS-1$
//            .setParameter("naa", LocalDateTime.now())
//            .setParameter("oppgaveId", oppgaveIder)
//            .getResultList()
//
//    }
//
//    override fun opprettOppgave(oppgave: Oppgave): Oppgave {
//        internLagre(oppgave)
//        entityManager.refresh(oppgave)
//        return oppgave
//    }
//
//    override fun opprettTilbakekrevingEgenskaper(egenskaper: TilbakekrevingOppgave): TilbakekrevingOppgave {
//        internLagre(egenskaper)
//        entityManager.refresh(egenskaper)
//        return egenskaper
//    }
//
//
//    @Deprecated("Bruk gjenåpneOppgaveForEksternId(Long) i stedet")
//    override fun gjenåpneOppgave(behandlingId: Long?): Oppgave? {
//        val oppgaver = hentOppgaver(behandlingId)
//        val sisteOppgave = oppgaver.stream()
//            .max(Comparator.comparing(Function<Oppgave, Any> { Oppgave.getOpprettetTidspunkt() }))
//            .orElse(null)
//        if (sisteOppgave != null) {
//            sisteOppgave!!.gjenåpneOppgave()
//            internLagre(sisteOppgave)
//            entityManager.refresh(sisteOppgave)
//        }
//        return sisteOppgave
//    }
//
//    override fun gjenåpneOppgaveForEksternId(eksternId: UUID): Oppgave? {
//        val oppgaver = hentOppgaverForEksternId(eksternId)
//        val sisteOppgave = oppgaver.stream()
//            .max(Comparator.comparing(Function<Oppgave, Any> { Oppgave.getOpprettetTidspunkt() }))
//            .orElse(null)
//        if (sisteOppgave != null) {
//            sisteOppgave!!.gjenåpneOppgave()
//            internLagre(sisteOppgave)
//            entityManager.refresh(sisteOppgave)
//        }
//        return sisteOppgave
//    }
//
//
//    @Deprecated("Bruk avsluttOppgaveForEksternId(Long) i stedet")
//    override fun avsluttOppgave(behandlingId: Long?) {
//        val oppgaver = hentOppgaver(behandlingId)
//        if (oppgaver.isEmpty()) {
//            return
//        }
//        val nyesteOppgave = oppgaver.stream()
//            .max(Comparator.comparing(Function<Oppgave, Any> { Oppgave.getOpprettetTidspunkt() }))
//            .orElse(null)
//        frigiEventuellReservasjon(nyesteOppgave.getReservasjon())
//        nyesteOppgave.avsluttOppgave()
//        internLagre(nyesteOppgave)
//        entityManager.refresh(nyesteOppgave)
//    }
//
//    override fun avsluttOppgaveForEksternId(eksternId: UUID) {
//        val oppgaver = hentOppgaverForEksternId(eksternId)
//        if (oppgaver.isEmpty()) {
//            return
//        }
//        val nyesteOppgave = oppgaver.stream()
//            .max(Comparator.comparing(Function<Oppgave, Any> { Oppgave.getOpprettetTidspunkt() }))
//            .orElse(null)
//        frigiEventuellReservasjon(nyesteOppgave.getReservasjon())
//        nyesteOppgave.avsluttOppgave()
//        internLagre(nyesteOppgave)
//        entityManager.refresh(nyesteOppgave)
//    }
//
//    private fun frigiEventuellReservasjon(reservasjon: Reservasjon?) {
//        if (reservasjon != null && reservasjon!!.erAktiv()) {
//            reservasjon!!.frigiReservasjon("Oppgave avsluttet")
//            lagre(reservasjon)
//            lagre(ReservasjonEventLogg(reservasjon))
//        }
//    }
//
//    override fun hentSisteReserverteOppgaver(uid: String): List<Oppgave> {
//        return entityManager.createQuery(
//                "SELECT o FROM Oppgave o " +
//                        "INNER JOIN Reservasjon r ON r.oppgave = o " +
//                        "WHERE upper(r.reservertAv) = upper( :uid ) ORDER BY coalesce(r.endretTidspunkt, r.opprettetTidspunkt) DESC ",
//                Oppgave::class.java
//            ) //$NON-NLS-1$
//            .setParameter("uid", uid).setMaxResults(10).getResultList()
//    }
//
//    override fun lagre(oppgaveEgenskap: OppgaveEgenskap) {
//        internLagre(oppgaveEgenskap)
//        refresh(oppgaveEgenskap.getOppgave())
//    }
//
//    override fun lagre(eventmottakFeillogg: EventmottakFeillogg) {
//        internLagre(eventmottakFeillogg)
//    }
//
//
//    @Deprecated("Bruk hentEventerForEksternId(Long) i stedet")
//    override fun hentEventer(behandlingId: Long?): List<OppgaveEventLogg> {
//        return entityManager.createQuery(
//                "FROM oppgaveEventLogg oel " + "where oel.behandlingId = :behandlingId ORDER BY oel.id desc",
//                OppgaveEventLogg::class.java
//            )
//            .setParameter("behandlingId", behandlingId).getResultList()
//    }
//
//    override fun hentEventerForEksternId(eksternId: UUID): List<OppgaveEventLogg> {
//        return entityManager.createQuery(
//                "FROM oppgaveEventLogg oel " + "where oel.eksternId = :eksternId ORDER BY oel.id desc",
//                OppgaveEventLogg::class.java
//            )
//            .setParameter("eksternId", eksternId).getResultList()
//    }
//
//    override fun hentOppgaveEgenskaper(oppgaveId: Long?): List<OppgaveEgenskap> {
//        return entityManager.createQuery(
//                "FROM OppgaveEgenskap oe " + "where oe.oppgaveId = :oppgaveId ORDER BY oe.id desc",
//                OppgaveEgenskap::class.java
//            )
//            .setParameter("oppgaveId", oppgaveId).getResultList()
//    }
//
//    override fun lagre(oppgaveEventLogg: OppgaveEventLogg) {
//        internLagre(oppgaveEventLogg)
//    }
//
//    override fun lagre(reservasjonEventLogg: ReservasjonEventLogg) {
//        internLagre(reservasjonEventLogg)
//    }
//
//
//    @Deprecated("Bruk hentOppgaverForEksternId(Long) i stedet")
//    private fun hentOppgaver(behandlingId: Long?): List<Oppgave> {
//        return entityManager.createQuery(
//                SELECT_FRA_OPPGAVE + "WHERE o.behandlingId = :behandlingId ",
//                Oppgave::class.java
//            )
//            .setParameter("behandlingId", behandlingId)
//            .getResultList()
//    }
//
//    private fun hentOppgaverForEksternId(eksternId: UUID): List<Oppgave> {
//        return entityManager.createQuery(SELECT_FRA_OPPGAVE + "WHERE o.eksternId = :eksternId ", Oppgave::class.java)
//            .setParameter("eksternId", eksternId)
//            .getResultList()
//    }
//
//    override fun settSortering(sakslisteId: Long?, sortering: String) {
//        entityManager.persist(
//            entityManager.find(OppgaveFiltreringOppdaterer::class.java, sakslisteId)
//                .endreSortering(sortering)
//                .endreErDynamiskPeriode(false)
//                .endreFomDato(null)
//                .endreTomDato(null)
//                .endreFraVerdi(null)
//                .endreTilVerdi(null)
//        )
//        entityManager.flush()
//    }
//
//    override fun settSorteringTidsintervallDato(oppgaveFiltreringId: Long?, fomDato: LocalDate, tomDato: LocalDate) {
//        entityManager.persist(
//            entityManager.find(OppgaveFiltreringOppdaterer::class.java, oppgaveFiltreringId)
//                .endreFomDato(fomDato)
//                .endreTomDato(tomDato)
//        )
//        entityManager.flush()
//    }
//
//    override fun settSorteringNumeriskIntervall(oppgaveFiltreringId: Long?, fra: Long?, til: Long?) {
//        entityManager.persist(
//            entityManager.find(OppgaveFiltreringOppdaterer::class.java, oppgaveFiltreringId)
//                .endreFraVerdi(fra)
//                .endreTilVerdi(til)
//        )
//        entityManager.flush()
//    }
//
//    override fun settSorteringTidsintervallValg(oppgaveFiltreringId: Long?, erDynamiskPeriode: Boolean) {
//        entityManager.persist(
//            entityManager.find(OppgaveFiltreringOppdaterer::class.java, oppgaveFiltreringId)
//                .endreErDynamiskPeriode(erDynamiskPeriode)
//                .endreFomDato(null)
//                .endreTomDato(null)
//                .endreFraVerdi(null)
//                .endreTilVerdi(null)
//        )
//        entityManager.flush()
//    }
//
//
//    private fun internLagre(objektTilLagring: Any) {
//        entityManager.persist(objektTilLagring)
//        entityManager.flush()
//    }
//
//    companion object {
//
//        private val COUNT_FRA_OPPGAVE = "SELECT count(1) from Oppgave o "
//        private val SELECT_FRA_OPPGAVE = "SELECT o from Oppgave o "
//        private val COUNT_FRA_TILBAKEKREVING_OPPGAVE = "SELECT count(1) from TilbakekrevingOppgave o "
//        private val SELECT_FRA_TILBAKEKREVING_OPPGAVE = "SELECT o from TilbakekrevingOppgave o "
//
//        private val SORTERING = "ORDER BY "
//        private val BEHANDLINGSFRIST = "o.behandlingsfrist"
//        private val BEHANDLINGOPPRETTET = "o.behandlingOpprettet"
//        private val FORSTE_STONADSDAG = "o.forsteStonadsdag"
//        private val BELOP = "o.belop"
//        private val FEILUTBETALINGSTART = "o.feilutbetalingstart"
//        private val OPPGAVEFILTRERING_SORTERING_NAVN = "ORDER BY l.navn"
//
//        private fun finnBrukernavn(): String {
//            val brukerident = SubjectHandler.getSubjectHandler().getUid()
//            return if (brukerident != null) brukerident!!.toUpperCase() else BRUKERNAVN_NÅR_SIKKERHETSKONTEKST_IKKE_FINNES
//        }
//    }
//
//}
