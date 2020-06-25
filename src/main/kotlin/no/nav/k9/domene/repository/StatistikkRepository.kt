package no.nav.k9.domene.repository

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.k9.aksjonspunktbehandling.objectMapper
import no.nav.k9.domene.modell.BehandlingType
import no.nav.k9.tjenester.avdelingsleder.nokkeltall.AlleFerdigstilteOppgaver
import no.nav.k9.tjenester.avdelingsleder.nokkeltall.AlleFerdigstilteOppgaverDto
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
}
