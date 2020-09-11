package no.nav.k9.domene.repository

import io.ktor.util.*
import kotlinx.coroutines.channels.Channel
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.k9.aksjonspunktbehandling.objectMapper
import no.nav.k9.domene.modell.KøSortering
import no.nav.k9.domene.modell.OppgaveKø
import no.nav.k9.integrasjon.abac.IPepClient
import no.nav.k9.tjenester.sse.Melding
import no.nav.k9.tjenester.sse.SseEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import javax.sql.DataSource

class OppgaveKøRepository(
    private val dataSource: DataSource,
    private val oppgaveKøOppdatert: Channel<UUID>,
    private val refreshKlienter: Channel<SseEvent>,
    private val pepClient: IPepClient
) {
    private val log: Logger = LoggerFactory.getLogger(OppgaveKøRepository::class.java)
    suspend fun hent(): List<OppgaveKø> {
        val skjermet = pepClient.harTilgangTilKode6()
        log.error("Skjermet " + skjermet)
        val json: List<String> = using(sessionOf(dataSource)) {
            it.run(
                queryOf(
                    "select data from oppgaveko where skjermet = :skjermet",
                    mapOf("skjermet" to skjermet)
                )
                    .map { row ->
                        row.string("data")
                    }.asList
            )
        }
        return json.map { s -> objectMapper().readValue(s, OppgaveKø::class.java) }.toList()
    }

    fun hentIkkeTaHensyn(): List<OppgaveKø> {
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

    @KtorExperimentalAPI
    suspend fun lagre(
        uuid: UUID,
        refresh: Boolean = false,
        f: (OppgaveKø?) -> OppgaveKø
    ) {
        val kode6 = pepClient.harTilgangTilKode6()
        using(sessionOf(dataSource)) {
            it.transaction { tx ->
                val run = tx.run(
                    queryOf(
                        "select data from oppgaveko where id = :id and skjermet = :skjermet for update",
                        mapOf("id" to uuid.toString(), "skjermet" to kode6)
                    )
                        .map { row ->
                            row.string("data")
                        }.asSingle
                )
                val forrigeOppgavekø: OppgaveKø?
                var oppgaveKø = if (!run.isNullOrEmpty()) {
                    forrigeOppgavekø = objectMapper().readValue(run, OppgaveKø::class.java)
                    f(forrigeOppgavekø)
                } else {
                    f(null)
                }
                oppgaveKø = oppgaveKø.copy(kode6 = kode6)
                //Sorter oppgaver
                if (oppgaveKø.sortering == KøSortering.FORSTE_STONADSDAG) {
                    oppgaveKø.oppgaverOgDatoer.sortBy { it.dato }
                }
                val json = objectMapper().writeValueAsString(oppgaveKø)
                tx.run(
                    queryOf(
                        """
                        insert into oppgaveko as k (id, data, skjermet)
                        values (:id, :data :: jsonb, :skjermet)
                        on conflict (id) do update
                        set data = :data :: jsonb, skjermet = :skjermet
                     """, mapOf("id" to uuid.toString(), "data" to json, "skjermet" to kode6)
                    ).asUpdate
                )

            }
        }
        if (refresh) {
            refreshKlienter.send(
                SseEvent(
                    objectMapper().writeValueAsString(
                        Melding(
                            "oppdaterTilBehandling",
                            uuid.toString()
                        )
                    )
                )
            )
        }
    }

    @KtorExperimentalAPI
    suspend fun lagreIkkeTaHensyn(
        uuid: UUID,
        refresh: Boolean = false,
        f: (OppgaveKø?) -> OppgaveKø
    ) {
        using(sessionOf(dataSource)) {
            it.transaction { tx ->
                val run = tx.run(
                    queryOf(
                        "select data from oppgaveko where id = :id  for update",
                        mapOf("id" to uuid.toString())
                    )
                        .map { row ->
                            row.string("data")
                        }.asSingle
                )
                val forrigeOppgavekø: OppgaveKø?
                val oppgaveKø = if (!run.isNullOrEmpty()) {
                    forrigeOppgavekø = objectMapper().readValue(run, OppgaveKø::class.java)
                    f(forrigeOppgavekø)
                } else {
                    f(null)
                }
                //Sorter oppgaver
                if (oppgaveKø.sortering == KøSortering.FORSTE_STONADSDAG) {
                    oppgaveKø.oppgaverOgDatoer.sortBy { it.dato }
                }
                val json = objectMapper().writeValueAsString(oppgaveKø)
                tx.run(
                    queryOf(
                        """
                        insert into oppgaveko as k (id, data, skjermet)
                        values (:id, :data :: jsonb, :skjermet)
                        on conflict (id) do update
                        set data = :data :: jsonb
                     """, mapOf("id" to uuid.toString(), "data" to json)
                    ).asUpdate
                )

            }
        }
        if (refresh) {
            refreshKlienter.send(
                SseEvent(
                    objectMapper().writeValueAsString(
                        Melding(
                            "oppdaterTilBehandling",
                            uuid.toString()
                        )
                    )
                )
            )
        }
    }

    suspend fun slett(id: UUID) {
        val skjermet = pepClient.harTilgangTilKode6()
        using(sessionOf(dataSource)) {
            it.transaction { tx ->
                tx.run(
                    queryOf(
                        """
                    delete from oppgaveko
                    where id = :id and skjermet = :skjermet
                 """, mapOf("id" to id.toString(), "skjermet" to skjermet)
                    ).asUpdate
                )
            }
        }
    }

    suspend fun oppdaterKøMedOppgaver(uuid: UUID) {
        oppgaveKøOppdatert.send(uuid)
    }

}
