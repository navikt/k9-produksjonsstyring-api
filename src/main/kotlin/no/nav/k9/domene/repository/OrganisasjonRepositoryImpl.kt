//package no.nav.k9.domene.repository
//
//import java.util.Optional
//
//import no.nav.vedtak.felles.jpa.HibernateVerktøy.hentEksaktResultat
//import no.nav.vedtak.felles.jpa.HibernateVerktøy.hentUniktResultat
//
//import javax.enterprise.context.ApplicationScoped
//import javax.inject.Inject
//import javax.persistence.EntityManager
//import javax.persistence.TypedQuery
//
//import no.nav.foreldrepenger.loslager.organisasjon.Avdeling
//import no.nav.foreldrepenger.loslager.organisasjon.Saksbehandler
//import no.nav.k9.domene.organisasjon.Avdeling
//import no.nav.k9.domene.organisasjon.Saksbehandler
//import no.nav.vedtak.felles.jpa.VLPersistenceUnit
//
//@ApplicationScoped
//class OrganisasjonRepositoryImpl : OrganisasjonRepository.kt {
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
//
//    private fun internLagre(skaLagres: Any) {
//        entityManager.persist(skaLagres)
//        entityManager.flush()
//    }
//
//    override fun hentAvdelingensSaksbehandlere(avdelingEnhet: String): List<Saksbehandler> {
//        val query = entityManager.createQuery(
//            "FROM avdeling a " + "WHERE a.avdelingEnhet = :avdelingEnhet",
//            Avdeling::class.java
//        )
//            .setParameter("avdelingEnhet", avdelingEnhet)
//        return hentEksaktResultat(query).getSaksbehandlere()
//    }
//
//    override fun lagre(saksbehandler: Saksbehandler) {
//        internLagre(saksbehandler)
//    }
//
//    override fun slettSaksbehandler(saksbehandlerIdent: String) {
//        entityManager.createNativeQuery("DELETE FROM SAKSBEHANDLER s " + "WHERE s.SAKSBEHANDLER_IDENT = :saksbehandlerIdent")
//            .setParameter("saksbehandlerIdent", saksbehandlerIdent)
//            .executeUpdate()
//    }
//
//    override fun hentSaksbehandler(saksbehandlerIdent: String): Saksbehandler {
//        return hentEksaktResultat(hentSaksbehandlerQuery(saksbehandlerIdent))
//    }
//
//    override fun hentMuligSaksbehandler(saksbehandlerIdent: String): Optional<Saksbehandler> {
//        return hentUniktResultat(hentSaksbehandlerQuery(saksbehandlerIdent))
//    }
//
//    override fun lagre(avdeling: Avdeling) {
//        internLagre(avdeling)
//    }
//
//    override fun refresh(avdeling: Avdeling) {
//        entityManager.refresh(avdeling)
//    }
//
//    private fun hentSaksbehandlerQuery(saksbehandlerIdent: String): TypedQuery<Saksbehandler> {
//        return entityManager.createQuery(
//            "FROM saksbehandler s WHERE upper(s.saksbehandlerIdent) = upper( :saksbehandlerIdent )",
//            Saksbehandler::class.java
//        )
//            .setParameter("saksbehandlerIdent", saksbehandlerIdent.toUpperCase())
//    }
//
//
//    override fun hentAvdeling(avdelingId: Long?): Avdeling {
//        val query = entityManager.createQuery("FROM avdeling a WHERE a.id = :avdelingId", Avdeling::class.java)
//            .setParameter("avdelingId", avdelingId)
//        return hentEksaktResultat(query)
//    }
//
//    override fun hentAvdelingFraEnhet(avdelingEnhet: String): Avdeling {
//        val query =
//            entityManager.createQuery("FROM avdeling a WHERE a.avdelingEnhet = :avdelingEnhet", Avdeling::class.java)
//                .setParameter("avdelingEnhet", avdelingEnhet)
//        return hentEksaktResultat(query)
//    }
//
//    override fun hentAvdelinger(): List<Avdeling> {
//        val listeTypedQuery = entityManager.createQuery("FROM avdeling ", Avdeling::class.java)
//        return listeTypedQuery.getResultList()
//    }
//
//    override fun hentAlleSaksbehandlere(): List<Saksbehandler> {
//        val query = entityManager.createQuery("FROM saksbehandler s", Saksbehandler::class.java)
//        return query.getResultList()
//    }
//
//
//}
