package no.nav.k9.domene.repository

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.k9.aksjonspunktbehandling.objectMapper
import no.nav.k9.domene.modell.KøSortering
import no.nav.k9.domene.modell.OppgaveKø
import no.nav.k9.tjenester.sse.SseEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import javax.sql.DataSource

class OppgaveKøRepository(
    private val dataSource: DataSource,
    private val oppgaveKøOppdatert: Channel<UUID>,
    private val oppgaveRepository: OppgaveRepository,
    private val refreshKlienter: Channel<SseEvent>
) {
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

    fun lagre(uuid: UUID, sorter: Boolean = true, refresh: Boolean = false, f: (OppgaveKø?) -> OppgaveKø) {
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
                var forrigeOppgavekø: String? = null
                val oppgaveKø = if (!run.isNullOrEmpty()) {
                    forrigeOppgavekø = run
                    f(objectMapper().readValue(run, OppgaveKø::class.java))
                } else {
                    f(null)
                }
                if (sorter) {
                    //Sorter oppgaver
                    if (oppgaveKø.sortering == KøSortering.FORSTE_STONADSDAG) {
                        oppgaveKø.oppgaver = oppgaveRepository.hentOppgaverSortertPåFørsteStønadsdag(oppgaveKø.oppgaver)
                            .map { id -> UUID.fromString(id) }.toMutableList()
                    }
                    if (oppgaveKø.sortering == KøSortering.OPPRETT_BEHANDLING) {
                        oppgaveKø.oppgaver = oppgaveRepository.hentOppgaverSortertPåOpprettetDato(oppgaveKø.oppgaver)
                            .map { id ->
                                UUID.fromString(id)
                            }.toMutableList()
                    }
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
                log.info("Refresh "+ refresh + " ulik kø " +  (forrigeOppgavekø != json))
                if (refresh && forrigeOppgavekø != json) {
                    runBlocking {
                        refreshKlienter.send(SseEvent("oppdaterTilBehandling"))
                    }
                }
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
        runBlocking {
            oppgaveKøOppdatert.send(uuid)
        }
    }

}
