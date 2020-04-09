package no.nav.k9.domene.repository

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.k9.aksjonspunktbehandling.objectMapper
import no.nav.k9.domene.lager.oppgave.Oppgave
import no.nav.k9.domene.lager.oppgave.OppgaveModell
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import javax.sql.DataSource


class OppgaveRepository(private val dataSource: DataSource) {
    private val log: Logger = LoggerFactory.getLogger(OppgaveRepository::class.java)
    fun hent(): MutableList<OppgaveModell> {
        val json: List<String> = using(sessionOf(dataSource)) {
            it.run(
                queryOf(
                    "select data from oppgave",
                    mapOf()
                )
                    .map { row ->
                        row.string("data")
                    }.asList
            )
        }
        val mutableList = mutableListOf<OppgaveModell>()
        for (s in json) {
            val oppgaveModell = objectMapper().readValue(s, OppgaveModell::class.java)
            mutableList.add(oppgaveModell)
        }
        log.info("Henter: " + mutableList.size + " oppgaver")
        return mutableList
    }

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

    fun lagre(uuid: UUID, f: (Oppgave?) -> Oppgave) {
        using(sessionOf(dataSource)) {
            it.transaction { tx ->
                val run = tx.run(
                    queryOf(
                        "select data from oppgave where id = :id for update",
                        mapOf("id" to uuid.toString())
                    )
                        .map { row ->
                            row.string("data")
                        }.asSingle
                )

                val oppgave = if (!run.isNullOrEmpty()) {
                    f(objectMapper().readValue(run, OppgaveModell::class.java).sisteOppgave())
                } else {
                    f(null)
                }
                val json = objectMapper().writeValueAsString(oppgave)
                tx.run(
                    queryOf(
                        """
                    insert into oppgave as k (id, data)
                    values (:id, :dataInitial :: jsonb)
                    on conflict (id) do update
                    set data = jsonb_set(k.data, '{oppgaver,999999}', :data :: jsonb, true)
                 """, mapOf("id" to uuid.toString(), "dataInitial" to "{\"oppgaver\": [$json]}", "data" to json)
                    ).asUpdate
                )

            }
        }
    }

    fun hentReserverteOppgaver(reservatør: String): List<OppgaveModell> {
        return hent().filter { oppgaveModell -> oppgaveModell.sisteOppgave().reservasjon?.reservertAv == reservatør }
    }
}