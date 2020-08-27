package no.nav.k9.domene.repository

import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.util.*
import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.k9.aksjonspunktbehandling.objectMapper
import no.nav.k9.domene.modell.Saksbehandler
import no.nav.k9.integrasjon.abac.IPepClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import javax.sql.DataSource

class SaksbehandlerRepository(
    private val dataSource: DataSource,
    private val pepClient: IPepClient
) {
    private val log: Logger = LoggerFactory.getLogger(SaksbehandlerRepository::class.java)
    private suspend fun lagreMedId(
        id: String,
        f: (Saksbehandler?) -> Saksbehandler
    ) {
        val skjermet = pepClient.harTilgangTilSkjermet()
        using(sessionOf(dataSource)) {
            it.transaction { tx ->
                val run = tx.run(
                    queryOf(
                        "select data from saksbehandler where saksbehandlerid = :saksbehandlerid and skjermet = :skjermet for update",
                        mapOf("saksbehandlerid" to id, "skjermet" to skjermet)
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
                        insert into saksbehandler as k (saksbehandlerid,navn, epost, data, skjermet)
                        values (:saksbehandlerid,:navn,:epost, :data :: jsonb, :skjermet)
                        on conflict (epost) do update
                        set data = :data :: jsonb, 
                            saksbehandlerid = :saksbehandlerid,
                            navn = :navn,
                            skjermet = :skjermet
                     """,
                        mapOf(
                            "saksbehandlerid" to id,
                            "epost" to saksbehandler.epost,
                            "navn" to saksbehandler.navn,
                            "data" to json,
                            "skjermet" to skjermet
                        )
                    ).asUpdate
                )
            }
        }
    }

    private suspend fun lagreMedEpost(
        epost: String,
        f: (Saksbehandler?) -> Saksbehandler
    ) {
        val erSkjermet = pepClient.harTilgangTilSkjermet()
        using(sessionOf(dataSource)) {
            it.transaction { tx ->
                val run = tx.run(
                    queryOf(
                        "select data from saksbehandler where lower(epost) = lower(:epost) and skjermet = :skjermet for update",
                        mapOf("epost" to epost, "skjermet" to erSkjermet)
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
                        insert into saksbehandler as k (saksbehandlerid, navn, epost, data, skjermet)
                        values (:saksbehandlerid,:navn,:epost, :data :: jsonb, :skjermet)
                        on conflict (epost) do update
                        set data = :data :: jsonb, 
                            saksbehandlerid = :saksbehandlerid,
                            navn = :navn,
                            skjermet = :skjermet
                     """,
                        mapOf(
                            "saksbehandlerid" to saksbehandler.brukerIdent,
                            "epost" to saksbehandler.epost.toLowerCase(),
                            "navn" to saksbehandler.navn,
                            "data" to json,
                            "skjermet" to erSkjermet
                        )
                    ).asUpdate
                )
            }
        }
    }


    suspend fun leggTilReservasjon(saksbehandlerid: String?, reservasjon: UUID) {
        if (saksbehandlerid == null) {
            return
        }
        lagreMedId(saksbehandlerid) { saksbehandler ->
            saksbehandler!!.reservasjoner.add(reservasjon)
            saksbehandler
        }
    }

    suspend fun fjernReservasjon(id: String?, reservasjon: UUID) {
        if (id == null) {
            return
        }
        lagreMedId(id) { saksbehandler ->
            saksbehandler!!.reservasjoner.remove(reservasjon)
            saksbehandler
        }
    }

    suspend fun addSaksbehandler(saksbehandler: Saksbehandler) {
        lagreMedEpost(saksbehandler.epost) {
            if (it == null) {
                saksbehandler
            } else {
                it.brukerIdent = saksbehandler.brukerIdent
                it.epost = saksbehandler.epost.toLowerCase()
                it.navn = saksbehandler.navn
                it.enhet = saksbehandler.enhet
                it
            }
        }
    }

    @KtorExperimentalAPI
    suspend fun finnSaksbehandlerMedEpost(epost: String): Saksbehandler? {
        val skjermet = pepClient.harTilgangTilSkjermet()
        val saksbehandler = using(sessionOf(dataSource)) {
            it.run(
                queryOf(
                    "select * from saksbehandler where lower(epost) = lower(:epost) and skjermet = :skjermet",
                    mapOf("epost" to epost, "skjermet" to skjermet)
                )
                    .map { row ->
                        mapSaksbehandler(row)
                    }.asSingle
            )
        }
        return saksbehandler
    }

    suspend fun finnSaksbehandlerMedIdent(ident: String): Saksbehandler? {
        val skjermet = pepClient.harTilgangTilSkjermet()
        val saksbehandler = using(sessionOf(dataSource)) {
            it.run(
                queryOf(
                    "select * from saksbehandler where lower(saksbehandlerid) = lower(:ident) and skjermet = :skjermet",
                    mapOf("ident" to ident, "skjermet" to skjermet)
                )
                    .map { row ->
                        mapSaksbehandler(row)
                    }.asSingle
            )
        }
        return saksbehandler
    }

     fun finnSaksbehandlerMedIdentIkkeSkjermet(ident: String): Saksbehandler? {
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
    
    @KtorExperimentalAPI
    suspend fun slettSaksbehandler(epost: String) {
        val skjermet = pepClient.harTilgangTilSkjermet()
        using(sessionOf(dataSource)) {
            it.transaction { tx ->
                tx.run(
                    queryOf(
                        """
                            delete from saksbehandler 
                            where lower(epost) = lower(:epost) and skjermet = :skjermet""",
                        mapOf("epost" to epost.toLowerCase(), "skjermet" to skjermet)
                    ).asUpdate
                )
            }
        }
    }

    @KtorExperimentalAPI
    suspend fun hentAlleSaksbehandlere(): List<Saksbehandler> {
        val skjermet = pepClient.harTilgangTilSkjermet()
        val identer = using(sessionOf(dataSource)) {
            it.run(
                queryOf(
                    "select * from saksbehandler where skjermet = :skjermet",
                    mapOf("skjermet" to skjermet)
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
