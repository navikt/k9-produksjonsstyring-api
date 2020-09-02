package no.nav.k9.domene.repository

import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.k9.aksjonspunktbehandling.objectMapper
import no.nav.k9.domene.lager.oppgave.Reservasjon
import no.nav.k9.tjenester.sse.Melding
import no.nav.k9.tjenester.sse.SseEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import javax.sql.DataSource

class ReservasjonRepository(
    private val oppgaveKøRepository: OppgaveKøRepository,
    private val oppgaveRepository: OppgaveRepository,
    private val saksbehandlerRepository: SaksbehandlerRepository,
    private val dataSource: DataSource,
    private val refreshKlienter: Channel<SseEvent>
) {
    private val log: Logger = LoggerFactory.getLogger(ReservasjonRepository::class.java)

    suspend fun hent(saksbehandlersIdent: String): List<Reservasjon> {
        val saksbehandler = saksbehandlerRepository.finnSaksbehandlerMedIdent(ident = saksbehandlersIdent)!!
        if (saksbehandler.reservasjoner.isEmpty()) {
            return emptyList()
        }
        return hent(saksbehandler.reservasjoner)
    }

    suspend fun hent(reservasjoner: Set<UUID>): List<Reservasjon> {
        return fjernReservasjonerSomIkkeLengerErAktive(
            hentReservasjoner(reservasjoner),
            oppgaveKøRepository,
            oppgaveRepository
        )
    }

    suspend fun fjernGamleReservasjoner(reservasjoner: Set<UUID>) {
        fjernReservasjonerSomIkkeLengerErAktive(
            hentReservasjoner(reservasjoner),
            oppgaveKøRepository,
            oppgaveRepository
        )
    }

    fun hentSelvOmDeIkkeErAktive(reservasjoner: Set<UUID>): List<Reservasjon> {
        return hentReservasjoner(reservasjoner)
    }

    private fun hentReservasjoner(set: Set<UUID>): List<Reservasjon> {
        val json: List<String> = using(sessionOf(dataSource)) {
            it.run(
                queryOf(
                    "select (data ::jsonb -> 'reservasjoner' -> -1) as data from reservasjon \n" +
                            "where id in " + set.joinToString(
                        separator = "\', \'",
                        prefix = "(\'",
                        postfix = "\')"
                    )
                )
                    .map { row ->
                        row.string("data")
                    }.asList
            )
        }
        val reservasjoner = json.map { s -> objectMapper().readValue(s, Reservasjon::class.java) }.toList()
        return reservasjoner
    }

    private suspend fun fjernReservasjonerSomIkkeLengerErAktive(
        reservasjoner: List<Reservasjon>,
        oppgaveKøRepository: OppgaveKøRepository,
        oppgaveRepository: OppgaveRepository
    ): List<Reservasjon> {
        reservasjoner.forEach { reservasjon ->
            if (!reservasjon.erAktiv()) {
                lagre(reservasjon.oppgave) {
                    it!!.reservertTil = null
                    it
                }
                saksbehandlerRepository.fjernReservasjon(reservasjon.reservertAv, reservasjon.oppgave)
                oppgaveKøRepository.hentIkkeTaHensyn().forEach { oppgaveKø ->
                    val oppgave = oppgaveRepository.hent(reservasjon.oppgave)
                    if (oppgaveKø.leggOppgaveTilEllerFjernFraKø(oppgave, this)) {
                        oppgaveKøRepository.lagre(oppgaveKø.id, refresh = true) {
                            it!!.leggOppgaveTilEllerFjernFraKø(
                                oppgave = oppgave,
                                reservasjonRepository = this
                            )
                            it
                        }
                    }
                }
            }
        }
        return reservasjoner.filter { it.erAktiv() }
    }

    fun hent(id: UUID): Reservasjon {
        val json: String? = using(sessionOf(dataSource)) {
            it.run(
                queryOf(
                    "select (data ::jsonb -> 'reservasjoner' -> -1) as data from reservasjon where id = :id",
                    mapOf("id" to id.toString())
                ).map { row ->
                    row.string("data")
                }.asSingle
            )
        }
        return objectMapper().readValue(json!!, Reservasjon::class.java)
    }

    fun hentMedHistorikk(id: UUID): List<Reservasjon> {
        val json: String? = using(sessionOf(dataSource)) {
            it.run(
                queryOf(
                    "select (data ::jsonb -> 'reservasjoner') as data from reservasjon where id = :id",
                    mapOf("id" to id.toString())
                ).map { row ->
                    row.string("data")
                }.asSingle
            )
        }
        return objectMapper().readValue(json!!)
    }

    fun finnes(id: UUID): Boolean {
        val json: String? = using(sessionOf(dataSource)) {
            it.run(
                queryOf(
                    "select (data ::jsonb -> 'reservasjoner' -> -1) as data from reservasjon where id = :id",
                    mapOf("id" to id.toString())
                ).map { row ->
                    row.string("data")
                }.asSingle
            )
        }
        return json != null
    }

    fun lagre(uuid: UUID, refresh: Boolean = false, f: (Reservasjon?) -> Reservasjon): Reservasjon {
        var reservasjon: Reservasjon? = null
        using(sessionOf(dataSource)) {
            it.transaction { tx ->
                val run = tx.run(
                    queryOf(
                        "select (data ::jsonb -> 'reservasjoner' -> -1) as data from reservasjon where id = :id for update",
                        mapOf("id" to uuid.toString())
                    )
                        .map { row ->
                            row.string("data")
                        }.asSingle
                )
                var forrigeReservasjon: String? = null
                reservasjon = if (!run.isNullOrEmpty()) {
                    forrigeReservasjon = run
                    f(objectMapper().readValue(run, Reservasjon::class.java))
                } else {
                    f(null)
                }
                val json = objectMapper().writeValueAsString(reservasjon)

                tx.run(
                    queryOf(
                        """
                    insert into reservasjon as k (id, data)
                    values (:id, :dataInitial :: jsonb)
                    on conflict (id) do update
                    set data = jsonb_set(k.data, '{reservasjoner,999999}', :data :: jsonb, true)
                 """, mapOf(
                            "id" to uuid.toString(),
                            "dataInitial" to "{\"reservasjoner\": [$json]}",
                            "data" to json
                        )
                    ).asUpdate
                )
                if (refresh && forrigeReservasjon != json) {
                    runBlocking { refreshKlienter.send((SseEvent(objectMapper().writeValueAsString(Melding("oppdaterReserverte"))))) }
                }
            }
        }
        return reservasjon!!
    }
}
