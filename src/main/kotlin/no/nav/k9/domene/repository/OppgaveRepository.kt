package no.nav.k9.domene.repository

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.k9.aksjonspunktbehandling.objectMapper
import no.nav.k9.domene.lager.oppgave.Oppgave
import no.nav.k9.domene.lager.oppgave.OppgaveModell
import java.util.*
import javax.sql.DataSource


class OppgaveRepository(private val dataSource: DataSource) {

    fun hent(uuid: UUID): OppgaveModell {
        val json: String? = using(sessionOf(dataSource)) {
            it.run(
                queryOf(
                    "select data from oppgave where id = :id",
                    mapOf("id" to uuid.toString())
                )
                    .map { row ->
                        row.string("data")
                    }.asSingle
            )
        }
        return objectMapper().readValue(json!!, OppgaveModell::class.java)

    }

    fun lagre(oppgave: Oppgave) {
        val json = objectMapper().writeValueAsString(oppgave)
        val id = oppgave.eksternId.toString()
        using(sessionOf(dataSource)) {
            it.run(
                queryOf(
                    """
                    insert into oppgave as k (id, data)
                    values (:id, :dataInitial :: jsonb)
                    on conflict (id) do update
                    set data = jsonb_set(k.data, '{oppgaver,999999}', :data :: jsonb, true)
                 """, mapOf("id" to id, "dataInitial" to "{\"oppgaver\": [$json]}", "data" to json)
                ).asUpdate
            )
        }
    }
}