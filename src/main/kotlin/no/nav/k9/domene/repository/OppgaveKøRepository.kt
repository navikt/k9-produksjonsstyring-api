package no.nav.k9.domene.repository

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.k9.aksjonspunktbehandling.objectMapper
import no.nav.k9.domene.modell.OppgaveKø
import no.nav.k9.tjenester.saksbehandler.saksliste.OppgavekøDto
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import javax.sql.DataSource

class OppgaveKøRepository(private val dataSource: DataSource) {
    private val log: Logger = LoggerFactory.getLogger(OppgaveKøRepository::class.java)
    fun hent(): MutableList<OppgaveKø> {
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
        val mutableList = mutableListOf<OppgaveKø>()
        for (s in json) {
            val oppgaveModell = objectMapper().readValue(s, OppgaveKø::class.java)
            mutableList.add(oppgaveModell)
        }
        return mutableList
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

    fun lagre(oppgaveKø: OppgaveKø) {
        using(sessionOf(dataSource)) {
            it.transaction { tx ->
                val json = objectMapper().writeValueAsString(oppgaveKø)
                tx.run(
                    queryOf(
                        """
                    insert into oppgaveko as k (id, data)
                    values (:id, :data  :: jsonb)
                    on conflict (id) do update
                    set data = :data :: jsonb
                 """, mapOf("id" to oppgaveKø.id.toString(), "data" to json)
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
}
