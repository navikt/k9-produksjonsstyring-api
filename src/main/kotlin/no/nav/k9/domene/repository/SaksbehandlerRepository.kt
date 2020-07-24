package no.nav.k9.domene.repository

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
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
    private fun lagreMedId(
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
                    f(null)
                }

                val json = objectMapper().writeValueAsString(saksbehandler)
                tx.run(
                    queryOf(
                        """
                        insert into saksbehandler as k (saksbehandlerid,navn, epost, data)
                        values (:saksbehandlerid,:navn,:epost, :data :: jsonb)
                        on conflict (epost) do update
                        set data = :data :: jsonb
                     """,
                        mapOf(
                            "saksbehandlerid" to id,
                            "epost" to saksbehandler.epost,
                            "navn" to saksbehandler.navn,
                            "data" to json
                        )
                    ).asUpdate
                )
            }
        }
    }

    private fun lagreMedEpost(
        epost: String,
        f: (Saksbehandler?) -> Saksbehandler
    ) {
        using(sessionOf(dataSource)) {
            it.transaction { tx ->
                val run = tx.run(
                    queryOf(
                        "select data from saksbehandler where epost = :epost for update",
                        mapOf("epost" to epost)
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
                    f(null)
                }

                val json = objectMapper().writeValueAsString(saksbehandler)
                tx.run(
                    queryOf(
                        """
                        insert into saksbehandler as k (saksbehandlerid, navn, epost, data)
                        values (:saksbehandlerid,:navn,:epost, :data :: jsonb)
                        on conflict (epost) do update
                        set data = :data :: jsonb
                     """,
                        mapOf(
                            "saksbehandlerid" to saksbehandler.brukerIdent,
                            "epost" to saksbehandler.epost.toLowerCase(),
                            "navn" to saksbehandler.navn,
                            "data" to json
                        )
                    ).asUpdate
                )
            }
        }
    }


    fun leggTilReservasjon(saksbehandlerid: String?, reservasjon: UUID) {
        if (saksbehandlerid == null) {
            return
        }
        lagreMedId(saksbehandlerid) { saksbehandler ->
            saksbehandler!!.reservasjoner.add(reservasjon)
            saksbehandler
        }
    }

    fun fjernReservasjon(id: String?, reservasjon: UUID) {
        if (id == null) {
            return
        }
        lagreMedId(id) { saksbehandler ->
            saksbehandler!!.reservasjoner.remove(reservasjon)
            saksbehandler
        }
    }

    fun addSaksbehandler(saksbehandler: Saksbehandler) {
        lagreMedEpost(saksbehandler.epost) {
            if (it == null) {
                saksbehandler
            } else {
                it.brukerIdent = saksbehandler.brukerIdent
                it.epost = saksbehandler.epost
                it.navn = saksbehandler.navn
                it.enhet = saksbehandler.enhet
                it
            }
        }
    }

    fun finnSaksbehandlerMedEpost(epost: String): Saksbehandler? {
        val saksbehandler = using(sessionOf(dataSource)) {
            it.run(
                queryOf(
                    "select * from saksbehandler where lower(epost) = lower(:epost)",
                    mapOf("epost" to epost)
                )
                    .map { row ->
                        mapSaksbehandler(row)
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
                        mapSaksbehandler(row)
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
                        mapSaksbehandler(row)
                    }.asList
            )
        }
        log.info("Henter " + identer.size + " saksbehandlere")

        return identer
    }

    private fun mapSaksbehandler(row: Row): Saksbehandler {
        val data = row.stringOrNull("data")
        return if (data == null) {
            Saksbehandler(
                row.stringOrNull("saksbehandlerid"),
                row.stringOrNull("navn"),
                row.string("epost").toLowerCase(),
                reservasjoner = mutableSetOf(),
                enhet = null
            )
        } else {
            Saksbehandler(
                brukerIdent = objectMapper().readValue<Saksbehandler>(data).brukerIdent,
                navn = objectMapper().readValue<Saksbehandler>(data).navn,
                epost = row.string("epost").toLowerCase(),
                reservasjoner = objectMapper().readValue<Saksbehandler>(data).reservasjoner,
                enhet = objectMapper().readValue<Saksbehandler>(data).enhet
            )
        }
    }
}
