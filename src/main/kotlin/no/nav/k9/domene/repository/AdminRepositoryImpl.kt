//package no.nav.k9.domene.repository
//
//import no.nav.foreldrepenger.loslager.oppgave.EventmottakFeillogg
//import no.nav.foreldrepenger.loslager.oppgave.Oppgave
//import no.nav.foreldrepenger.loslager.oppgave.OppgaveEventLogg
//import no.nav.foreldrepenger.loslager.oppgave.Reservasjon
//import no.nav.foreldrepenger.loslager.oppgave.TilbakekrevingOppgave
//import no.nav.k9.domene.lager.oppgave.Oppgave
//import no.nav.vedtak.felles.jpa.VLPersistenceUnit
//import org.slf4j.Logger
//import org.slf4j.LoggerFactory
//
//import javax.enterprise.context.ApplicationScoped
//import javax.inject.Inject
//import javax.persistence.EntityManager
//import javax.persistence.NoResultException
//import java.util.UUID
//
//@ApplicationScoped
//class AdminRepositoryImpl : AdminRepository {
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
//    override fun deaktiverSisteOppgave(behandlingId: Long?) {
//        entityManager.createNativeQuery("UPDATE OPPGAVE o SET o.AKTIV = 'N' WHERE o.BEHANDLING_ID = :behandlingId")
//            .setParameter("behandlingId", behandlingId)
//            .executeUpdate()
//        entityManager.flush()
//    }
//
//    override fun hentSisteOppgave(behandlingId: Long?): Oppgave? {
//        var oppgave: Oppgave? = null
//        try {
//            oppgave = entityManager.createQuery(
//                    "Select o FROM Oppgave o where o.behandlingId = :behandlingId ORDER BY o.opprettetTidspunkt desc",
//                    Oppgave::class.java
//                )
//                .setParameter("behandlingId", behandlingId)
//                .setMaxResults(1).getSingleResult()
//            entityManager.refresh(oppgave)
//        } catch (nre: NoResultException) {
//            log.info("Fant ingen oppgave tilknyttet behandling med id {}", behandlingId, nre)
//        }
//
//        return oppgave
//    }
//
//    override fun hentSisteTilbakekrevingOppgave(uuid: UUID): TilbakekrevingOppgave? {
//        var oppgave: TilbakekrevingOppgave? = null
//        try {
//            oppgave = entityManager.createQuery(
//                    "Select to FROM TilbakekrevingOppgave to where to.eksternId = :eksternId ORDER BY to.opprettetTidspunkt desc",
//                    TilbakekrevingOppgave::class.java
//                )
//                .setParameter("eksternId", uuid)
//                .setMaxResults(1).getSingleResult()
//            entityManager.refresh(oppgave)
//        } catch (nre: NoResultException) {
//            log.info("Fant ingen oppgave tilknyttet behandling med id {}", uuid, nre)
//        }
//
//        return oppgave
//    }
//
//    override fun hentEventer(behandlingId: Long?): List<OppgaveEventLogg> {
//        return entityManager.createQuery(
//                "Select o FROM oppgaveEventLogg o " + "where o.behandlingId = :behandlingId ORDER BY o.opprettetTidspunkt desc",
//                OppgaveEventLogg::class.java
//            )
//            .setParameter("behandlingId", behandlingId).getResultList()
//    }
//
//    override fun hentAlleAktiveOppgaver(): List<Oppgave> {
//        return entityManager.createQuery(
//            "Select o FROM Oppgave o where o.aktiv = true ORDER BY o.opprettetTidspunkt desc",
//            Oppgave::class.java
//        ).getResultList()
//    }
//
//    override fun hentAlleMeldingerFraFeillogg(): List<EventmottakFeillogg> {
//        return entityManager.createQuery(
//            "Select ef FROM eventmottakFeillogg ef where ef.status = :status",
//            EventmottakFeillogg::class.java
//        ).setParameter("status", EventmottakFeillogg.Status.FEILET).getResultList()
//    }
//
//    override fun markerFerdig(feilloggId: Long?) {
//        entityManager.persist(
//            entityManager
//                .find(EventmottakFeillogg::class.java, feilloggId)
//                .markerFerdig()
//        )
//        entityManager.flush()
//    }
//
//    override fun hentAlleOppgaverForBehandling(behandlingId: Long?): List<Oppgave> {
//        return entityManager.createQuery(
//                "Select o FROM Oppgave o where o.behandlingId = :behandlingId ORDER BY o.opprettetTidspunkt desc",
//                Oppgave::class.java
//            )
//            .setParameter("behandlingId", behandlingId)
//            .getResultList()
//    }
//
//    override fun deaktiverOppgave(oppgaveId: Long?): Oppgave {
//        val oppgave = hentOppgave(oppgaveId)
//        val reservasjon = oppgave.getReservasjon()
//        if (reservasjon != null && reservasjon!!.erAktiv()) {
//            reservasjon!!.frigiReservasjon("Oppgave er avsluttet fra admin REST-tjeneste")
//            lagreReservasjon(reservasjon)
//        }
//        oppgave.avsluttOppgave()
//        internLagre(oppgave)
//        entityManager.refresh(oppgave)
//        return hentOppgave(oppgaveId)
//    }
//
//    override fun aktiverOppgave(oppgaveId: Long?): Oppgave {
//        val oppgave = hentOppgave(oppgaveId)
//        oppgave.gjenåpneOppgave()
//        internLagre(oppgave)
//        entityManager.refresh(oppgave)
//        return hentOppgave(oppgaveId)
//    }
//
//    private fun hentOppgave(oppgaveId: Long?): Oppgave {
//        val oppgave = entityManager.createQuery("Select o FROM Oppgave o where o.id = :oppgaveId", Oppgave::class.java)
//            .setParameter("oppgaveId", oppgaveId)
//            .setMaxResults(1).getSingleResult()
//        entityManager.refresh(oppgave)
//        return oppgave
//    }
//
//    private fun lagreReservasjon(reservasjon: Reservasjon) {
//        internLagre(reservasjon)
//    }
//
//    private fun internLagre(objektTilLagring: Any) {
//        entityManager.persist(objektTilLagring)
//        entityManager.flush()
//    }
//
//    companion object {
//
//        private val log = LoggerFactory.getLogger(AdminRepositoryImpl::class.java)
//    }
//}
