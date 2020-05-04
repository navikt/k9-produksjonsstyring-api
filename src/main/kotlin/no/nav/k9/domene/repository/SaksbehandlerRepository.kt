package no.nav.k9.domene.repository

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.k9.aksjonspunktbehandling.objectMapper
import no.nav.k9.domene.modell.Saksbehandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.sql.DataSource

class SaksbehandlerRepository(
    private val dataSource: DataSource) {
    private val log: Logger = LoggerFactory.getLogger(SaksbehandlerRepository::class.java)

    fun addSaksbehandler(saksbehandler: Saksbehandler) {
        using(sessionOf(dataSource)) { it ->
            it.transaction { tx ->
                tx.run(
                    queryOf(
                        """
                            insert into saksbehandler (saksbehandlerid, navn, epost)
                            values(:ident, :navn, :epost)""",
                        mapOf("ident" to saksbehandler.brukerIdent, "navn" to saksbehandler.navn, "epost" to saksbehandler.epost)
                    ).asUpdate
                )
            }
        }
    }

    fun finnSaksbehandler(epost: String): Saksbehandler? {
        val saksbehandler = using(sessionOf(dataSource)) {
            it.run(
                queryOf(
                    "select * from saksbehandler where epost = :epost",
                    mapOf("epost" to epost)
                )
                    .map { row ->
                        Saksbehandler(
                            row.string("saksbehandlerid"),
                            row.string("navn"),
                            row.string("epost"))
                    }.asSingle
            )
        }
        return saksbehandler
    }

    fun hentAlleSaksbehandlere(): List<Saksbehandler> {
        val identer: List<String> = using(sessionOf(dataSource)) {
            it.run(
                queryOf(
                    "select * from saksbehandler",
                    mapOf()
                )
                    .map { row ->
                        row.string("saksbehandlerid")
                    }.asList
            )
        }

        log.info("Henter " + identer.size + " saksbehanlere")

        return identer.map { i -> objectMapper().readValue(i, Saksbehandler::class.java) }
    }
}
