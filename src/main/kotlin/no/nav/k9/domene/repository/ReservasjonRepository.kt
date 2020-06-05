package no.nav.k9.domene.repository

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.k9.aksjonspunktbehandling.objectMapper
import no.nav.k9.domene.lager.oppgave.Reservasjon
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import javax.sql.DataSource

class ReservasjonRepository(private val oppgaveKøRepository: OppgaveKøRepository, private val oppgaveRepository: OppgaveRepository,private val dataSource: DataSource) {
    private val log: Logger = LoggerFactory.getLogger(ReservasjonRepository::class.java)
    fun hent(oppgaveKøRepository: OppgaveKøRepository, oppgaveRepository: OppgaveRepository): List<Reservasjon> {
        val json: List<String> = using(sessionOf(dataSource)) {
            it.run(
                queryOf(
                    "select (data ::jsonb -> 'reservasjoner' -> -1) as data from reservasjon \n" +
                            "where not (data ::jsonb -> 'reservasjoner' -> -1 ?? 'aktiv')::BOOLEAN\n" +
                            "   or (data ::jsonb -> 'reservasjoner' -> -1 -> 'aktiv')::BOOLEAN",
                    mapOf()
                )
                    .map { row ->
                        row.string("data")
                    }.asList
            )
        }
        val reservasjoner = json.map { s -> objectMapper().readValue(s, Reservasjon::class.java) }.toList()
        return fjernReservasjonerSomIkkeLengerErAktive(reservasjoner, oppgaveKøRepository, oppgaveRepository)
    }
    
    fun hent(saksbehandlersIdent: String): List<Reservasjon> {
        val json: List<String> = using(sessionOf(dataSource)) {
            it.run(
                queryOf(
                    "select (data ::jsonb -> 'reservasjoner' -> -1) as data from reservasjon \n" +
                            "where (not (data ::jsonb -> 'reservasjoner' -> -1 ?? 'aktiv')::BOOLEAN\n" +
                            "or (data ::jsonb -> 'reservasjoner' -> -1 -> 'aktiv')::BOOLEAN) " +
                            "and (data ::jsonb -> 'reservasjoner' -> -1 ->> 'reservertAv') = :saksbehandlersIdent",
                    mapOf("saksbehandlersIdent" to saksbehandlersIdent)
                )
                    .map { row ->
                        row.string("data")
                    }.asList
            )
        }
        val reservasjoner = json.map { s -> objectMapper().readValue(s, Reservasjon::class.java) }.toList()
        return fjernReservasjonerSomIkkeLengerErAktive(reservasjoner, oppgaveKøRepository, oppgaveRepository)
    }

    private fun fjernReservasjonerSomIkkeLengerErAktive(
        reservasjoner: List<Reservasjon>,
        oppgaveKøRepository: OppgaveKøRepository,
        oppgaveRepository: OppgaveRepository
    ): List<Reservasjon> {
        reservasjoner.forEach { reservasjon ->
            if (!reservasjon.erAktiv()) {
                lagre(reservasjon.oppgave) {
                    it!!.aktiv = false
                    it
                }
                oppgaveKøRepository.hent().forEach { oppgaveKø ->
                    oppgaveKøRepository.lagre(oppgaveKø.id) {
                        it!!.leggOppgaveTilEllerFjernFraKø(
                            oppgave = oppgaveRepository.hent(reservasjon.oppgave),
                            reservasjonRepository = this
                        )
                        it
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
        val json: List<String> = using(sessionOf(dataSource)) {
            it.run(
                    queryOf(
                            "select (data ::jsonb -> 'reservasjoner') as data from reservasjon where id = :id",
                            mapOf("id" to id.toString())
                    ).map { row ->
                        row.string("data")
                    }.asList
            )
        }
        return json.map { s -> objectMapper().readValue(s, Reservasjon::class.java) }.toList()
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
        return json!= null
    }

    fun lagre(uuid: UUID, f: (Reservasjon?) -> Reservasjon): Reservasjon {
        var reservasjon : Reservasjon? = null
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

                val data = if (!run.isNullOrEmpty()) {
                    f(objectMapper().readValue(run, Reservasjon::class.java))
                } else {
                    f(null)
                }
                reservasjon = data
                val json = objectMapper().writeValueAsString(data)

                tx.run(
                    queryOf(
                        """
                    insert into reservasjon as k (id, data)
                    values (:id, :dataInitial :: jsonb)
                    on conflict (id) do update
                    set data = jsonb_set(k.data, '{reservasjoner,999999}', :data :: jsonb, true)
                 """, mapOf("id" to uuid.toString(),
                            "dataInitial" to "{\"reservasjoner\": [$json]}",
                            "data" to json)
                    ).asUpdate
                )
            }
        }
        return reservasjon!!
    }
}
