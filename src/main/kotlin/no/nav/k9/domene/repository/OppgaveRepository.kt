package no.nav.k9.domene.repository

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.k9.aksjonspunktbehandling.objectMapper
import no.nav.k9.domene.lager.oppgave.Oppgave
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import javax.sql.DataSource


class OppgaveRepository(
    private val dataSource: DataSource,
    private val oppgaveOppdatert: Channel<Oppgave>
) {
    private val log: Logger = LoggerFactory.getLogger(OppgaveRepository::class.java)
    fun hent(): List<Oppgave> {
        var spørring = System.currentTimeMillis()
        val json: List<String> = using(sessionOf(dataSource)) {
            it.run(
                queryOf(
                    "select (data ::jsonb -> 'oppgaver' -> -1) as data from oppgave",
                    mapOf()
                )
                    .map { row ->
                        row.string("data")
                    }.asList
            )
        }
        spørring = System.currentTimeMillis() - spørring
        val serialisering = System.currentTimeMillis()
        val list = json.map { s -> objectMapper().readValue(s, Oppgave::class.java) }.toList()

        log.info("Henter: " + list.size + " oppgaver" + " serialisering: " + (System.currentTimeMillis() - serialisering) + " spørring: " + spørring)
        return list
    }

    fun hent(uuid: UUID): Oppgave {
        val json: String? = using(sessionOf(dataSource)) {
            it.run(
                queryOf(
                    "select (data ::jsonb -> 'oppgaver' -> -1) as data from oppgave where id = :id",
                    mapOf("id" to uuid.toString())
                )
                    .map { row ->
                        row.string("data")
                    }.asSingle
            )
        }
        return objectMapper().readValue(json!!, Oppgave::class.java)

    }

    fun lagre(uuid: UUID, f: (Oppgave?) -> Oppgave) {
        using(sessionOf(dataSource)) {
            it.transaction { tx ->
                val run = tx.run(
                    queryOf(
                        "select (data ::jsonb -> 'oppgaver' -> -1) as data from oppgave where id = :id for update",
                        mapOf("id" to uuid.toString())
                    )
                        .map { row ->
                            row.string("data")
                        }.asSingle
                )

                val oppgave = if (!run.isNullOrEmpty()) {
                    f(objectMapper().readValue(run, Oppgave::class.java))
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
                runBlocking {
                    oppgaveOppdatert.send(oppgave)
                }
            }
        }

    }

    fun hentOppgaverSortertPåOpprettetDato(oppgaveider: List<UUID>): List<Oppgave> {
        var spørring = System.currentTimeMillis()

        val session = sessionOf(dataSource)
        val json: List<String> = using(session) {
            //language=PostgreSQL
            it.run(
                queryOf(
                    "select (data ::jsonb -> 'oppgaver' -> -1) as data from oppgave " +
                            "where (data ::jsonb -> 'oppgaver' -> -1 ->> 'eksternId') in (${IntRange(
                                0,
                                oppgaveider.size - 1
                            ).map { t -> ":p$t" }.joinToString()}) " +
                            "order by (data ::jsonb -> 'oppgaver' -> -1 -> 'behandlingOpprettet')",
                    IntRange(0, oppgaveider.size-1).map { t -> "p$t" to oppgaveider[t].toString() }.toMap()
                )
                    .map { row ->
                        row.string("data")
                    }.asList
            )
        }
        spørring = System.currentTimeMillis() - spørring
        val serialisering = System.currentTimeMillis()
        val list = json.map { s -> objectMapper().readValue(s, Oppgave::class.java) }.toList()

        log.info("Henter oppgaver basert på opprettetDato: " + list.size + " oppgaver" + " serialisering: " + (System.currentTimeMillis() - serialisering) + " spørring: " + spørring)
        return list
    }

    fun hentOppgaverSortertPåFørsteStønadsdag(oppgaveider: List<UUID>): List<Oppgave> {
        var spørring = System.currentTimeMillis()
        val session = sessionOf(dataSource)
        val json: List<String> = using(session) {
            //language=PostgreSQL
            it.run(
                queryOf(
                    "select (data ::jsonb -> 'oppgaver' -> -1) as data from oppgave " +
                            "where (data ::jsonb -> 'oppgaver' -> -1 ->> 'eksternId') in (${IntRange(
                                0,
                                oppgaveider.size - 1
                            ).map { t -> ":p$t" }.joinToString()}) " +
                            "order by (data ::jsonb -> 'oppgaver' -> -1 -> 'forsteStonadsdag')",
                    IntRange(0, oppgaveider.size-1).map { t -> "p$t" to oppgaveider[t].toString() }.toMap()
                )
                    .map { row ->
                        row.string("data")
                    }.asList
            )
        }
        spørring = System.currentTimeMillis() - spørring
        val serialisering = System.currentTimeMillis()
        val list = json.map { s -> objectMapper().readValue(s, Oppgave::class.java) }.toList()

        log.info("Henter oppgaver basert på forsteStonadsdag: " + list.size + " oppgaver" + " serialisering: " + (System.currentTimeMillis() - serialisering) + " spørring: " + spørring)
        return list
    }

    fun hentOppgaverMedAktorId(aktørId: String) = hent()
        .filter { it.aktorId == aktørId }

    fun hentOppgaverMedSaksnummer(saksnummer: String) = hent()
        .filter { it.fagsakSaksnummer == saksnummer }

    internal fun hentAktiveOppgaverTotalt(): Int {
        var spørring = System.currentTimeMillis()
        val count: Int? = using(sessionOf(dataSource)) {
            it.run(
                queryOf(
                    "select count(*) as count from oppgave where (data ::jsonb -> 'oppgaver' -> -1 -> 'aktiv') ::boolean",
                    mapOf()
                )
                    .map { row ->
                        row.int("count")
                    }.asSingle
            )
        }
        spørring = System.currentTimeMillis() - spørring
        log.info("Teller aktive oppgaver: $spørring ms")
        return count!!
    }

    internal fun hentAktiveOppgaver(): List<Oppgave> {
        var spørring = System.currentTimeMillis()
        val json: List<String> = using(sessionOf(dataSource)) {
            it.run(
                queryOf(
                    "select (data ::jsonb -> 'oppgaver' -> -1) as data from oppgave where (data ::jsonb -> 'oppgaver' -> -1 -> 'aktiv') ::boolean ",
                    mapOf()
                )
                    .map { row ->
                        row.string("data")
                    }.asList
            )
        }
        spørring = System.currentTimeMillis() - spørring
        val serialisering = System.currentTimeMillis()
        val list = json.map { s -> objectMapper().readValue(s, Oppgave::class.java) }.toList()

        log.info("Henter aktive oppgaver: " + list.size + " oppgaver" + " serialisering: " + (System.currentTimeMillis() - serialisering) + " spørring: " + spørring)
        return list
    }
}
