package no.nav.k9.domene.repository

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.k9.domene.modell.Saksbehandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.sql.DataSource

class SaksbehandlerRepository(
    private val dataSource: DataSource) {
    private val log: Logger = LoggerFactory.getLogger(SaksbehandlerRepository::class.java)

    fun addSaksbehandler(saksbehandler: Saksbehandler) {
        using(sessionOf(dataSource)) {
            it.transaction { tx ->
                tx.run(
                    queryOf(
                        """
                            insert into saksbehandler (saksbehandlerid, navn, epost)
                            values(:ident, :navn, :epost)
                            on conflict (epost) do update
                            set navn = :navn, saksbehandlerid = :ident
                            """,
                        mapOf("ident" to saksbehandler.brukerIdent, "navn" to saksbehandler.navn, "epost" to saksbehandler.epost.toLowerCase())
                    ).asUpdate
                )
            }
        }
    }

    fun finnSaksbehandlerMedEpost(epost: String): Saksbehandler? {
        val saksbehandler = using(sessionOf(dataSource)) {
            it.run(
                queryOf(
                    "select * from saksbehandler where epost = :epost",
                    mapOf("epost" to epost.toLowerCase())
                )
                    .map { row ->
                        Saksbehandler(
                            row.stringOrNull("saksbehandlerid"),
                            row.stringOrNull("navn"),
                            row.string("epost").toLowerCase())
                    }.asSingle
            )
        }
        return saksbehandler
    }

    fun finnSaksbehandlerMedIdent(ident: String): Saksbehandler? {
        val saksbehandler = using(sessionOf(dataSource)) {
            it.run(
                queryOf(
                    "select * from saksbehandler where lower(saksbehandlerid) = lower(:ident)",
                    mapOf("ident" to ident)
                )
                    .map { row ->
                        Saksbehandler(
                            row.stringOrNull("saksbehandlerid"),
                            row.stringOrNull("navn"),
                            row.string("epost").toLowerCase())
                    }.asSingle
            )
        }
        return saksbehandler
    }

    fun slettSaksbehandler(epost: String) {
        using(sessionOf(dataSource)) {
            it.transaction { tx ->
                tx.run(
                    queryOf(
                        """
                            delete from saksbehandler 
                            where epost = :epost """,
                        mapOf("epost" to epost.toLowerCase())
                    ).asUpdate
                )
            }
        }
    }

    fun hentAlleSaksbehandlere(): List<Saksbehandler> {
        val identer = using(sessionOf(dataSource)) {
            it.run(
                queryOf(
                    "select * from saksbehandler",
                    mapOf()
                )
                    .map { row ->
                        Saksbehandler(
                            row.stringOrNull("saksbehandlerid"),
                            row.stringOrNull("navn"),
                            row.string("epost").toLowerCase())
                    }.asList
            )
        }
        log.info("Henter " + identer.size + " saksbehandlere")

        return identer
    }
}
