package no.nav.k9.domene.repository

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.k9.aksjonspunktbehandling.objectMapper
import no.nav.k9.domene.modell.OppgaveKø
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import javax.sql.DataSource

class OppgaveKøRepository(private val dataSource: DataSource, private val oppgaveRepository: OppgaveRepository) {
    private val log: Logger = LoggerFactory.getLogger(OppgaveKøRepository::class.java)
    fun hent(): List<OppgaveKø> {
        val json: List<String> = using(sessionOf(dataSource)) {
            it.run(
                queryOf(
                    "select data from oppgaveko",
                    mapOf()
                )
                    .map { row ->
                        row.string("data")
                    }.asList
            )
        }
        return json.map { s -> objectMapper().readValue(s, OppgaveKø::class.java) }.toList()
    }

    fun hentOppgavekø(id: UUID): OppgaveKø {
        val json: String? = using(sessionOf(dataSource)) {
            it.run(
                queryOf(
                    "select data from oppgaveko where id = :id",
                    mapOf("id" to id.toString())
                ).map { row ->
                    row.string("data")
                }.asSingle
            )
        }
        return objectMapper().readValue(json!!, OppgaveKø::class.java)

    }

    fun lagre(uuid: UUID, f: (OppgaveKø?) -> OppgaveKø) {
        using(sessionOf(dataSource)) {
            it.transaction { tx ->
                val run = tx.run(
                    queryOf(
                        "select data from oppgaveko where id = :id for update",
                        mapOf("id" to uuid.toString())
                    )
                        .map { row ->
                            row.string("data")
                        }.asSingle
                )

                val oppgaveKø = if (!run.isNullOrEmpty()) {
                    f(objectMapper().readValue(run, OppgaveKø::class.java))
                } else {
                    f(null)
                }
                val json = objectMapper().writeValueAsString(oppgaveKø)
                tx.run(
                    queryOf(
                        """
                        insert into oppgaveko as k (id, data)
                        values (:id, :data :: jsonb)
                        on conflict (id) do update
                        set data = :data :: jsonb
                     """, mapOf("id" to uuid.toString(), "data" to json)
                    ).asUpdate
                )
            }
        }
    }

    fun slett(id: UUID) {
        using(sessionOf(dataSource)) {
            it.transaction { tx ->
                tx.run(
                    queryOf(
                        """
                    delete from oppgaveko
                    where id = :id
                 """, mapOf("id" to id.toString())
                    ).asUpdate
                )
            }
        }
    }

    fun oppdaterKøMedOppgaver(uuid: UUID) {
        val hentAktiveOppgaver = oppgaveRepository.hentAktiveOppgaver(Int.MAX_VALUE)
        lagre(uuid) { oppgaveKø ->
            oppgaveKø!!.oppgaver = mutableListOf()
            for (oppgaveModell in hentAktiveOppgaver) {
                oppgaveKø.leggOppgaveTilEllerFjernFraKø(oppgave = oppgaveModell.sisteOppgave())
            }
            oppgaveKø
        }
    }
}
