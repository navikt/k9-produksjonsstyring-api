package no.nav.k9.domene.repository

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.k9.aksjonspunktbehandling.objectMapper
import no.nav.k9.domene.lager.oppgave.Oppgave
import no.nav.k9.domene.modell.BehandlingType
import no.nav.k9.domene.modell.FagsakYtelseType
import no.nav.k9.tjenester.avdelingsleder.nokkeltall.AlleOppgaverDto
import no.nav.k9.tjenester.avdelingsleder.nokkeltall.AlleOppgaverPerDato
import no.nav.k9.tjenester.saksbehandler.oppgave.BehandletOppgave
import no.nav.k9.utils.Cache
import no.nav.k9.utils.CacheObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import javax.sql.DataSource


class OppgaveRepository(
    private val dataSource: DataSource
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

    fun hentEldsteOppgaveTid(): String {
        val json: String? = using(sessionOf(dataSource)) {
            it.run(
                queryOf(
                    "select (data ::jsonb -> 'oppgaver' -> -1 -> 'eventTid')" +
                            " from oppgave order by (data ::jsonb -> 'oppgaver' -> -1 -> 'eventTid') limit 1 ",
                    mapOf()
                )
                    .map { row ->
                        row.string("data")
                    }.asSingle
            )
        }
        return  json!!
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
            }
        }

    }

    fun hentOppgaver(oppgaveider: Collection<UUID>): List<Oppgave> {
        val oppgaveiderList = oppgaveider.toList()
        if (oppgaveider.isEmpty()) {
            return emptyList()
        }
        var spørring = System.currentTimeMillis()
        val session = sessionOf(dataSource)
        val json: List<String> = using(session) {
            //language=PostgreSQL
            it.run(
                queryOf(
                    "select (data ::jsonb -> 'oppgaver' -> -1) as data from oppgave " +
                            "where id in (${IntRange(
                                0,
                                oppgaveiderList.size - 1
                            ).map { t -> ":p$t" }.joinToString()}) ",
                    IntRange(0, oppgaveiderList.size - 1).map { t -> "p$t" to oppgaveiderList[t].toString() }.toMap()
                )
                    .map { row ->
                        row.string("data")
                    }.asList
            )
        }
        spørring = System.currentTimeMillis() - spørring
        val serialisering = System.currentTimeMillis()
        val list = json.map { s -> objectMapper().readValue(s, Oppgave::class.java) }.toList().sortedBy { oppgave -> oppgave.behandlingOpprettet }

        log.info("Henter oppgaver: " + list.size + " oppgaver" + " serialisering: " + (System.currentTimeMillis() - serialisering) + " spørring: " + spørring)
        return list
    }

    fun hentAlleOppgaverUnderArbeid(): List<AlleOppgaverDto> {
        val json = using(sessionOf(dataSource)) {
            it.run(
                queryOf(
                    """
                        select count(*) as antall,
                        (data ::jsonb -> 'oppgaver' -> -1 -> 'fagsakYtelseType' ->> 'kode') as fagsakYtelseType,
                        (data ::jsonb -> 'oppgaver' -> -1 -> 'behandlingType' ->> 'kode') as behandlingType,
                        not (data ::jsonb -> 'oppgaver' -> -1 -> 'tilBeslutter') ::boolean as tilBehandling
                        from oppgave o where (data ::jsonb -> 'oppgaver' -> -1 -> 'aktiv') ::boolean
                        group by  behandlingType, fagsakYtelseType, tilBehandling
                    """.trimIndent(),
                    mapOf()
                )
                    .map { row ->
                        AlleOppgaverDto(
                            FagsakYtelseType.fraKode(row.string("fagsakYtelseType")),
                            BehandlingType.fraKode(row.string("behandlingType")),
                            row.boolean("tilBehandling"),
                            row.int("antall"))
                    }.asList
            )
        }
        return json
    }

    fun hentAlleOppgaverPerDato(): List<AlleOppgaverPerDato> {
        val json = using(sessionOf(dataSource)) {
            it.run(
                queryOf(
                    """
                        select count(*) as antall,
                        (data ::jsonb -> 'oppgaver' -> -1 -> 'fagsakYtelseType' ->> 'kode') as fagsakYtelseType,
                        (data ::jsonb -> 'oppgaver' -> -1 -> 'behandlingType' ->> 'kode') as behandlingType,
                        (data ::jsonb -> 'oppgaver' -> -1 ->> 'behandlingOpprettet') ::date as opprettetDato
                        from oppgave o where (data ::jsonb -> 'oppgaver' -> -1 -> 'aktiv') ::boolean
                        and (data ::jsonb -> 'oppgaver' -> -1 ->> 'behandlingOpprettet')::date <= current_date 
                        and (data ::jsonb -> 'oppgaver' -> -1 ->> 'behandlingOpprettet')::date >= (current_date - '28 days' ::interval)
                        group by  opprettetDato, behandlingType, fagsakYtelseType
                    """.trimIndent(),
                    mapOf()
                )
                    .map { row ->
                        AlleOppgaverPerDato(
                            FagsakYtelseType.fraKode(row.string("fagsakYtelseType")),
                            BehandlingType.fraKode(row.string("behandlingType")),
                            row.localDate("opprettetDato"),
                            row.int("antall"))
                    }.asList
            )
        }
        return json
    }

    fun hentOppgaverMedAktorId(aktørId: String): List<Oppgave> {
        val json: List<String> = using(sessionOf(dataSource)) {
            //language=PostgreSQL
            it.run(
                queryOf(
                    "select (data ::jsonb -> 'oppgaver' -> -1) as data from oppgave where (data ::jsonb -> 'oppgaver' -> -1 ->> 'aktorId') = :aktorId",
                    mapOf("aktorId" to aktørId)
                )
                    .map { row ->
                        row.string("data")
                    }.asList
            )
        }
        return json.map { s -> objectMapper().readValue(s, Oppgave::class.java) }.toList()
    }

    fun hentOppgaveMedSaksnummer(saksnummer: String): Oppgave? {
        val json: String = using(sessionOf(dataSource)) {
            //language=PostgreSQL
            it.run(
                queryOf(
                    "select (data ::jsonb -> 'oppgaver' -> -1) as data from oppgave where lower(data ::jsonb -> 'oppgaver' -> -1 ->> 'fagsakSaksnummer') = lower(:saksnummer)",
                    mapOf("saksnummer" to saksnummer)
                )
                    .map { row ->
                        row.string("data")
                    }.asSingle
            )
        }
            ?: return null
        return objectMapper().readValue(json, Oppgave::class.java)
    }

    private val hentAktiveOppgaverTotaltCache = Cache<Int>()
    internal fun hentAktiveOppgaverTotalt(): Int {
        val cacheObject = hentAktiveOppgaverTotaltCache.get("default")
        if (cacheObject != null) {
            return cacheObject.value
        }
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
        hentAktiveOppgaverTotaltCache.set("default", CacheObject(count!!))
        return count!!
    }

    internal fun hentInaktiveOppgaverTotalt(): Int {
        var spørring = System.currentTimeMillis()
        val count: Int? = using(sessionOf(dataSource)) {
            it.run(
                queryOf(
                    "select count(*) as count from oppgave where not (data ::jsonb -> 'oppgaver' -> -1 -> 'fagsakYtelseType' ->> 'kode' = 'FRISINN')  and (data ::jsonb -> 'oppgaver' -> -1 -> 'behandlingStatus' ->> 'kode' = 'AVSLU') ::boolean",
                    mapOf()
                )
                    .map { row ->
                        row.int("count")
                    }.asSingle
            )
        }
        spørring = System.currentTimeMillis() - spørring
        log.info("Teller inaktive oppgaver: $spørring ms")
        return count!!
    }

    internal fun hentAutomatiskProsesserteTotalt(): Int {
        var spørring = System.currentTimeMillis()
        val count: Int? = using(sessionOf(dataSource)) {
            //language=PostgreSQL
            it.run(
                queryOf(
                    "select count(*) as count from oppgave o left join reservasjon r using (id) where not (o.data ::jsonb -> 'oppgaver' -> -1 -> 'fagsakYtelseType' ->> 'kode' = 'FRISINN')  and (o.data ::jsonb -> 'oppgaver' -> -1 -> 'behandlingStatus' ->> 'kode' = 'AVSLU') and r.id is null",
                    mapOf()
                )
                    .map { row ->
                        row.int("count")
                    }.asSingle
            )
        }
        spørring = System.currentTimeMillis() - spørring
        log.info("Teller autmatiske oppgaver: $spørring ms")
        return count!!
    }

    internal fun hentBeslutterTotalt(): Int {
        var spørring = System.currentTimeMillis()
        val count: Int? = using(sessionOf(dataSource)) {
            //language=PostgreSQL
            it.run(
                queryOf(
                    "select count(*) as count from oppgave o left join reservasjon r using (id)\n" +
                            "where not (o.data ::jsonb -> 'oppgaver' -> -1 -> 'fagsakYtelseType' ->> 'kode' = 'FRISINN') and (o.data ::jsonb -> 'oppgaver' -> -1 -> 'behandlingStatus' ->> 'kode' = 'AVSLU') and r.id is not null and exists(\n" +
                            "    select 1 from jsonb_array_elements(o.data -> 'oppgaver') elem\n" +
                            "    where (elem -> 'tilBeslutter') :: BOOLEAN\n" +
                            "    )",
                    mapOf()
                )
                    .map { row ->
                        row.int("count")
                    }.asSingle
            )
        }
        spørring = System.currentTimeMillis() - spørring
        log.info("Teller autmatiske oppgaver: $spørring ms")
        return count!!
    }
    private val aktiveOppgaverCache = Cache<List<Oppgave>>()
    internal fun hentAktiveOppgaver(): List<Oppgave> {
        val cacheObject = aktiveOppgaverCache.get("default")
        if (cacheObject != null) {
            return cacheObject.value
        }
        
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
        aktiveOppgaverCache.set("default", CacheObject(list))
        return list
    }
}
