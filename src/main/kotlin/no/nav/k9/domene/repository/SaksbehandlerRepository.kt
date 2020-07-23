package no.nav.k9.domene.repository

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.k9.aksjonspunktbehandling.objectMapper
import no.nav.k9.domene.modell.Saksbehandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import javax.sql.DataSource

class SaksbehandlerRepository(
    private val dataSource: DataSource
) {
    private val log: Logger = LoggerFactory.getLogger(SaksbehandlerRepository::class.java)
    private fun lagre(
        id: String,
        f: (Saksbehandler?) -> Saksbehandler
    ) {
        using(sessionOf(dataSource)) {
            it.transaction { tx ->
                val run = tx.run(
                    queryOf(
                        "select data from saksbehandler where saksbehandlerid = :saksbehandlerid for update",
                        mapOf("saksbehandlerid" to id)
                    )
                        .map { row ->
                            row.stringOrNull("data")
                        }.asSingle
                )
                val forrige: Saksbehandler?
                val saksbehandler = if (!run.isNullOrEmpty()) {
                    forrige = objectMapper().readValue(run, Saksbehandler::class.java)
                    f(forrige)
                } else {
                    finnSaksbehandlerMedIdent(ident = id)
                }

                val json = objectMapper().writeValueAsString(saksbehandler)
                tx.run(
                    queryOf(
                        """
                        insert into saksbehandler as k (saksbehandlerid, epost, data)
                        values (:saksbehandlerid,:epost, :data :: jsonb)
                        on conflict (epost) do update
                        set data = :data :: jsonb
                     """, mapOf("saksbehandlerid" to id,"epost" to saksbehandler!!.epost, "data" to json)
                    ).asUpdate
                )
            }
        }
    }

    fun leggTilReservasjon(saksbehandlerid: String?, reservasjon: UUID) {
        if (saksbehandlerid == null) {
            return
        }
        lagre(saksbehandlerid) { saksbehandler ->
            saksbehandler!!.reservasjoner.add(reservasjon)
            saksbehandler
        }
    }

    fun fjernReservasjon(id: String?, reservasjon: UUID) {
        if (id == null) {
            return
        }
        lagre(id) { saksbehandler ->
            saksbehandler!!.reservasjoner.remove(reservasjon)
            saksbehandler
        }
    }

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
                        mapOf(
                            "ident" to saksbehandler.brukerIdent,
                            "navn" to saksbehandler.navn,
                            "epost" to saksbehandler.epost.toLowerCase()
                        )
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
                        val data = row.stringOrNull("data")
                        if (data == null) {
                            Saksbehandler(
                                row.stringOrNull("saksbehandlerid"),
                                row.stringOrNull("navn"),
                                row.string("epost").toLowerCase(),
                                reservasjoner = mutableSetOf()
                            )
                        } else {
                            Saksbehandler(
                                row.stringOrNull("saksbehandlerid"),
                                row.stringOrNull("navn"),
                                row.string("epost").toLowerCase(),
                                reservasjoner = objectMapper().readValue<Saksbehandler>(data).reservasjoner
                            )
                        }
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
                        val data = row.stringOrNull("data")
                        if (data == null) {
                            Saksbehandler(
                                row.stringOrNull("saksbehandlerid"),
                                row.stringOrNull("navn"),
                                row.string("epost").toLowerCase(),
                                reservasjoner = mutableSetOf()
                            )
                        } else {
                            Saksbehandler(
                                row.stringOrNull("saksbehandlerid"),
                                row.stringOrNull("navn"),
                                row.string("epost").toLowerCase(),
                                reservasjoner = objectMapper().readValue<Saksbehandler>(data).reservasjoner
                            )
                        }
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
                        val data = row.stringOrNull("data")
                        if (data == null) {
                            Saksbehandler(
                                row.stringOrNull("saksbehandlerid"),
                                row.stringOrNull("navn"),
                                row.string("epost").toLowerCase(),
                                reservasjoner = mutableSetOf()
                            )
                        } else {
                            Saksbehandler(
                                row.stringOrNull("saksbehandlerid"),
                                row.stringOrNull("navn"),
                                row.string("epost").toLowerCase(),
                                reservasjoner = objectMapper().readValue<Saksbehandler>(data).reservasjoner
                            )
                        }
                    }.asList
            )
        }
        log.info("Henter " + identer.size + " saksbehandlere")

        return identer
    }
}
