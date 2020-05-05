package no.nav.k9.domene.repository

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
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


class OppgaveRepository(
    private val dataSource: DataSource,
    private val oppgaveOppdatert: Channel<Oppgave>
) {
    private val log: Logger = LoggerFactory.getLogger(OppgaveRepository::class.java)
    fun hent(): List<OppgaveModell> {
        var spørring = System.currentTimeMillis()
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
        spørring = System.currentTimeMillis() - spørring
        val serialisering = System.currentTimeMillis()
        val list = json.map { s -> objectMapper().readValue(s, OppgaveModell::class.java) }.toList()

        log.info("Henter: " + list.size + " oppgaver" + " serialisering: " + (System.currentTimeMillis() - serialisering) + " spørring: " + spørring)
        return list
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
                runBlocking {
                    oppgaveOppdatert.send(oppgave)
                }
            }
        }

    }

    fun hentOppgaverMedAktorId(aktørId: String) = hent()
        .filter { it.sisteOppgave().aktorId == aktørId }

    fun hentOppgaverMedSaksnummer(saksnummer: String) = hent()
        .filter { it.sisteOppgave().fagsakSaksnummer == saksnummer }

    fun hentReserverteOppgaver(reservatør: String): List<OppgaveModell> {
        return hentAktiveOppgaver(Int.MAX_VALUE)
            .filter { o -> o.sisteOppgave().reservasjon?.reservertAv == reservatør }
    }

    internal fun hentAktiveOppgaverTotalt(): Int {
        var spørring = System.currentTimeMillis()
        val count: Int? = using(sessionOf(dataSource)) {
            it.run(
                queryOf(
                    "select count(*) as count  from oppgave where (data ::jsonb -> 'oppgaver' -> 0 -> 'aktiv') ::boolean",
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

    internal fun hentAktiveOppgaver(limit: Int): List<OppgaveModell> {
        var spørring = System.currentTimeMillis()
        val json: List<String> = using(sessionOf(dataSource)) {
            it.run(
                queryOf(
                    "select data from oppgave where (data ::jsonb -> 'oppgaver' -> 0 -> 'aktiv') ::boolean limit :limit",
                    mapOf("limit" to limit)
                )
                    .map { row ->
                        row.string("data")
                    }.asList
            )
        }
        spørring = System.currentTimeMillis() - spørring
        val serialisering = System.currentTimeMillis()
        val list = json.map { s -> objectMapper().readValue(s, OppgaveModell::class.java) }.toList()

        log.info("Henter aktive oppgaver: " + list.size + " oppgaver" + " serialisering: " + (System.currentTimeMillis() - serialisering) + " spørring: " + spørring)
        list.filter { o -> o.sisteOppgave().aktiv }
        return list
    }
}
