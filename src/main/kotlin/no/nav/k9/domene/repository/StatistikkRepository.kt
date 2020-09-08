package no.nav.k9.domene.repository

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.k9.aksjonspunktbehandling.objectMapper
import no.nav.k9.domene.lager.oppgave.Oppgave
import no.nav.k9.domene.modell.BehandlingType
import no.nav.k9.domene.modell.FagsakYtelseType
import no.nav.k9.tjenester.avdelingsleder.nokkeltall.AlleFerdigstilteOppgaver
import no.nav.k9.tjenester.avdelingsleder.nokkeltall.AlleOppgaverNyeOgFerdigstilte
import no.nav.k9.tjenester.saksbehandler.oppgave.BehandletOppgave
import java.time.LocalDate
import java.util.*
import javax.sql.DataSource

class StatistikkRepository(
    private val dataSource: DataSource
) {
    fun lagreBehandling(brukerIdent: String, f: (BehandletOppgave?) -> BehandletOppgave) {
        using(sessionOf(dataSource)) {
            it.transaction { tx ->
                val run = tx.run(
                    queryOf(
                        "select (data ::jsonb -> 'siste_behandlinger' -> -1) as data from siste_behandlinger where id = :id for update",
                        mapOf("id" to brukerIdent)
                    )
                        .map { row ->
                            row.string("data")
                        }.asSingle
                )

                val oppgave = if (!run.isNullOrEmpty()) {
                    f(objectMapper().readValue(run, BehandletOppgave::class.java))
                } else {
                    f(null)
                }
                val json = objectMapper().writeValueAsString(oppgave)

                tx.run(
                    queryOf(
                        """
                    insert into siste_behandlinger as k (id, data)
                    values (:id, :dataInitial :: jsonb)
                    on conflict (id) do update
                    set data = jsonb_set(k.data, '{siste_behandlinger,999999}', :data :: jsonb, true)
                 """, mapOf("id" to brukerIdent, "dataInitial" to "{\"siste_behandlinger\": [$json]}", "data" to json)
                    ).asUpdate
                )
            }
        }
    }

    fun hentBehandlinger(ident: String): List<BehandletOppgave> {
        val json = using(sessionOf(dataSource)) {
            it.run(
                queryOf(
                    """select  data , timestamp from (
                            select distinct on (eksternId) (data ::jsonb -> 'eksternId') as eksternId , (data ::jsonb -> 'timestamp') as timestamp, data from (
                            select jsonb_array_elements_text(data ::jsonb -> 'siste_behandlinger') as data
                            from siste_behandlinger where id = :id) as saker order by eksternId desc ) as s order by timestamp desc limit 10""".trimIndent(),
                    mapOf("id" to ident)
                )
                    .map { row ->
                        row.string("data")
                    }.asList
            )
        }
        return json.map { objectMapper().readValue(it, BehandletOppgave::class.java) }
    }

    fun lagreFerdigstilt(bt: String, eksternId: UUID) {
        using(sessionOf(dataSource)) {
            it.transaction { tx ->
                //language=PostgreSQL
                tx.run(
                    queryOf(
                        """insert into ferdigstilte_behandlinger as k (behandlingType, dato, data)
                                    values (:behandlingType, current_date, :dataInitial ::jsonb)
                                    on conflict (behandlingType, dato) do update
                                    set data = k.data || :data ::jsonb
                                 """, mapOf(
                            "behandlingType" to bt,
                            "dataInitial" to "[\"${eksternId}\"]",
                            "data" to "[\"$eksternId\"]"
                        )
                    ).asUpdate
                )
            }
        }
    }

    fun hentFerdigstilte(): List<AlleFerdigstilteOppgaver> {
        return using(sessionOf(dataSource)) {
            it.run(
                queryOf(
                    """
                            select behandlingtype, dato, jsonb_array_length(data) as antall
                            from ferdigstilte_behandlinger  where dato >= current_date - '7 days'::interval
                            group by behandlingtype,dato
                    """.trimIndent(),
                    mapOf()
                )
                    .map { row ->
                        AlleFerdigstilteOppgaver(
                            behandlingType = BehandlingType.fraKode(row.string("behandlingType")),
                            dato = row.localDate("dato"),
                            antall = row.int("antall")
                        )
                    }.asList
            )
        }
    }

    fun lagre(
        alleOppgaverNyeOgFerdigstilte: AlleOppgaverNyeOgFerdigstilte,
        f: (AlleOppgaverNyeOgFerdigstilte) -> AlleOppgaverNyeOgFerdigstilte
    ) {
        using(sessionOf(dataSource)) {
            it.transaction { tx ->
                val run = tx.run(
                    queryOf(
                        "select * from nye_og_ferdigstilte where behandlingType = :behandlingType and fagsakYtelseType = :fagsakYtelseType and dato = :dato for update",
                        mapOf(  "behandlingType" to alleOppgaverNyeOgFerdigstilte.behandlingType.kode,
                            "fagsakYtelseType" to alleOppgaverNyeOgFerdigstilte.fagsakYtelseType.kode,
                            "dato" to alleOppgaverNyeOgFerdigstilte.dato)
                    )
                        .map { row ->
                            AlleOppgaverNyeOgFerdigstilte(
                                behandlingType = BehandlingType.fraKode(row.string("behandlingType")),
                                fagsakYtelseType = FagsakYtelseType.fraKode(row.string("fagsakYtelseType")),
                                dato = row.localDate("dato"),
                                ferdigstilte = objectMapper().readValue(row.stringOrNull("ferdigstilte") ?: "[]"),
                                nye = objectMapper().readValue(row.stringOrNull("nye") ?: "[]")
                            )
                        }.asSingle
                )
                val alleOppgaverNyeOgFerdigstilteSomPersisteres = if (run != null) {
                    f(run)
                } else {
                    f(alleOppgaverNyeOgFerdigstilte)
                }

                tx.run(
                    queryOf(
                        """
                                    insert into nye_og_ferdigstilte as k (behandlingType, fagsakYtelseType, dato, nye,ferdigstilte)
                                    values (:behandlingType, :fagsakYtelseType, :dato, :nye ::jsonb, :ferdigstilte ::jsonb)
                                    on conflict (behandlingType, fagsakYtelseType, dato) do update
                                    set nye = :nye ::jsonb , ferdigstilte = :ferdigstilte ::jsonb 
                     """, mapOf(
                            "behandlingType" to alleOppgaverNyeOgFerdigstilteSomPersisteres.behandlingType.kode,
                            "fagsakYtelseType" to alleOppgaverNyeOgFerdigstilteSomPersisteres.fagsakYtelseType.kode,
                            "dato" to alleOppgaverNyeOgFerdigstilteSomPersisteres.dato,
                            "nye" to objectMapper().writeValueAsString(alleOppgaverNyeOgFerdigstilteSomPersisteres.nye),
                            "ferdigstilte" to objectMapper().writeValueAsString(alleOppgaverNyeOgFerdigstilteSomPersisteres.ferdigstilte)
                        )
                    ).asUpdate
                )

            }
        }
    }


    fun hentFerdigstilteOgNyeHistorikkPerAntallDager(antall: Int): List<AlleOppgaverNyeOgFerdigstilte> {
        return using(sessionOf(dataSource)) {
            //language=PostgreSQL
            it.run(
                queryOf(
                    """
                            select behandlingtype, dato, ferdigstilte, nye
                            from nye_og_ferdigstilte  where dato >= current_date - :antall::interval
                    """.trimIndent(),
                    mapOf("antall" to "\'${antall} days\'")
                )
                    .map { row ->
                        AlleOppgaverNyeOgFerdigstilte(
                            behandlingType = BehandlingType.fraKode(row.string("behandlingType")),
                            fagsakYtelseType = FagsakYtelseType.OMSORGSPENGER,
                            dato = row.localDate("dato"),
                            ferdigstilte = objectMapper().readValue(row.stringOrNull("ferdigstilte") ?: "[]"),
                            nye = objectMapper().readValue(row.stringOrNull("nye") ?: "[]")
                        )
                    }.asList
            )
        }
    }
    fun truncateNyeOgFerdigstilte(){
        using(sessionOf(dataSource)) {
            //language=PostgreSQL
            it.run(
                queryOf(
                    """
                            truncate nye_og_ferdigstilte
                    """.trimIndent(),
                    mapOf()
                ).asUpdate
            )
        }
    }
    fun hentFerdigstilteOgNyeHistorikkMedYtelsetype(antall: Int): List<AlleOppgaverNyeOgFerdigstilte> {
        val list = using(sessionOf(dataSource)) {
            //language=PostgreSQL
            it.run(
                queryOf(
                    """
                            select behandlingtype, fagsakYtelseType, dato, ferdigstilte, nye 
                            from nye_og_ferdigstilte  where dato >= current_date - :antall::interval
                            group by behandlingtype, fagsakYtelseType, dato
                    """.trimIndent(),
                    mapOf("antall" to "\'${antall} days\'")
                )
                    .map { row ->
                        AlleOppgaverNyeOgFerdigstilte(
                            behandlingType = BehandlingType.fraKode(row.string("behandlingType")),
                            fagsakYtelseType = FagsakYtelseType.fraKode(row.string("fagsakYtelseType")),
                            dato = row.localDate("dato"),
                            ferdigstilte = objectMapper().readValue(row.stringOrNull("ferdigstilte") ?: "[]"),
                            nye = objectMapper().readValue(row.stringOrNull("nye") ?: "[]")
                        )
                    }.asList
            )
        }
        val datoMap = list.groupBy { it.dato }
        val ret = mutableListOf<AlleOppgaverNyeOgFerdigstilte>()
        for (i in antall downTo 1) {
            val dato = LocalDate.now().minusDays(i.toLong())
            val defaultList = mutableListOf<AlleOppgaverNyeOgFerdigstilte>()
            for (behandlingType in BehandlingType.values()) {
                defaultList.addAll(tomListe(behandlingType, dato))
            }
            val dagensStatistikk = datoMap.getOrDefault(dato, defaultList)
            val behandlingsTypeMap = dagensStatistikk.groupBy { it.behandlingType }

            for (behandlingstype in BehandlingType.values()) {

                val perBehandlingstype =
                    behandlingsTypeMap.getOrDefault(behandlingstype, tomListe(behandlingstype, dato))
                val fagSakytelsesMap = perBehandlingstype.groupBy { it.fagsakYtelseType }
                for (fagsakYtelseType in FagsakYtelseType.values()) {
                    ret.addAll(
                        fagSakytelsesMap.getOrDefault(
                            fagsakYtelseType, listOf(
                                AlleOppgaverNyeOgFerdigstilte(
                                    fagsakYtelseType = fagsakYtelseType,
                                    behandlingType = behandlingstype,
                                    dato = dato
                                )
                            )
                        )
                    )
                }
            }
        }
        return ret
    }

    private fun tomListe(
        behandlingstype: BehandlingType,
        dato: LocalDate
    ): MutableList<AlleOppgaverNyeOgFerdigstilte> {
        val defaultList = mutableListOf<AlleOppgaverNyeOgFerdigstilte>()
        for (fagsakYtelseType in FagsakYtelseType.values()) {
            defaultList.add(
                AlleOppgaverNyeOgFerdigstilte(
                    fagsakYtelseType = fagsakYtelseType,
                    behandlingType = behandlingstype,
                    dato = dato
                )
            )
        }
        return defaultList
    }
}
