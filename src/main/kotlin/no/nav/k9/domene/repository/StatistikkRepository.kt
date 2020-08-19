package no.nav.k9.domene.repository

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.k9.aksjonspunktbehandling.objectMapper
import no.nav.k9.domene.modell.BehandlingType
import no.nav.k9.domene.modell.FagsakYtelseType
import no.nav.k9.tjenester.avdelingsleder.nokkeltall.AlleFerdigstilteOppgaver
import no.nav.k9.tjenester.avdelingsleder.nokkeltall.AlleOppgaverNyeOgFerdigstilte
import no.nav.k9.tjenester.saksbehandler.oppgave.BehandletOppgave
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
                                 """, mapOf("behandlingType" to bt,
                                            "dataInitial" to "[\"${eksternId}\"]",
                                            "data" to "[\"$eksternId\"]")
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

    fun lagreFerdigstiltHistorikk(bt: String, fyt: String, eksternId: UUID) {
        using(sessionOf(dataSource)) {
            it.transaction { tx ->
                //language=PostgreSQL
                tx.run(
                    queryOf(
                        """insert into nye_og_ferdigstilte as k (behandlingType, fagsakYtelseType, dato, ferdigstilte)
                                    values (:behandlingType, :fagsakYtelseType, current_date, :dataInitial ::jsonb)
                                    on conflict (behandlingType, fagsakYtelseType, dato) do update
                                    set ferdigstilte = k.ferdigstilte || :ferdigstilte ::jsonb
                                 """, mapOf("behandlingType" to bt,
                            "fagsakYtelseType" to fyt,
                            "dataInitial" to "[\"${eksternId}\"]",
                            "ferdigstilte" to "[\"$eksternId\"]")
                    ).asUpdate
                )
            }
        }
    }

    fun lagreNyHistorikk(bt: String, fyt: String, eksternId: UUID) {
        using(sessionOf(dataSource)) {
            it.transaction { tx ->
                //language=PostgreSQL
                tx.run(
                    queryOf(
                        """insert into nye_og_ferdigstilte as k (behandlingType, fagsakYtelseType, dato, nye)
                                    values (:behandlingType, :fagsakYtelseType, current_date, :dataInitial ::jsonb)
                                    on conflict (behandlingType, fagsakYtelseType, dato) do update
                                    set nye = k.nye || :nye ::jsonb
                                 """, mapOf("behandlingType" to bt,
                            "fagsakYtelseType" to fyt,
                            "dataInitial" to "[\"${eksternId}\"]",
                            "nye" to "[\"$eksternId\"]")
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
                            select behandlingtype, dato, ferdigstilte, jsonb_array_length(nye) as nye
                            from nye_og_ferdigstilte  where dato >= current_date - :antall::interval
                            group by behandlingtype, dato
                    """.trimIndent(),
                    mapOf("antall" to "\'${antall} days\'")
                )
                    .map { row ->
                        AlleOppgaverNyeOgFerdigstilte(
                            behandlingType = BehandlingType.fraKode(row.string("behandlingType")),
                            fagsakYtelseType = FagsakYtelseType.OMSORGSPENGER,
                            dato = row.localDate("dato"),
                            ferdigstilte = objectMapper().readValue(row.stringOrNull("ferdigstilte")?:"[]"),
                            nye = row.intOrNull("nye")?:0
                        )
                    }.asList
            )
        }
    }

    fun hentFerdigstilteOgNyeHistorikkMedYtelsetype(antall: Int): List<AlleOppgaverNyeOgFerdigstilte> {
        return using(sessionOf(dataSource)) {
            //language=PostgreSQL
            it.run(
                queryOf(
                    """
                            select behandlingtype, fagsakYtelseType, dato, ferdigstilte, jsonb_array_length(nye) as nye
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
                            ferdigstilte = objectMapper().readValue(row.stringOrNull("ferdigstilte")?:"[]"),
                            nye = row.intOrNull("nye")?:0
                        )
                    }.asList
            )
        }
    }
}
