package no.nav.k9.domene.repository

import com.fasterxml.jackson.core.type.TypeReference
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.k9.aksjonspunktbehandling.objectMapper
import no.nav.k9.domene.lager.oppgave.Oppgave
import no.nav.k9.domene.modell.BehandlingType
import no.nav.k9.domene.modell.FagsakYtelseType
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon
import no.nav.k9.tjenester.avdelingsleder.nokkeltall.AlleOppgaverDto
import no.nav.k9.tjenester.avdelingsleder.nokkeltall.AlleOppgaverPerDato
import no.nav.k9.tjenester.mock.Aksjonspunkt
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
        val list = json.map { s -> objectMapper().readValue(s, Oppgave::class.java) }.toList()

        log.info("Henter: " + list.size + " oppgaver" + " serialisering: " + (System.currentTimeMillis() - serialisering) + " spørring: " + spørring)
        return list
    }

    fun hent(uuid: UUID): Oppgave {
        try {
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
            return objectMapper().readValue(json!!, Oppgave::class.java)
        } catch (_: Exception) {
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
                    f(objectMapper().readValue(run, Oppgave::class.java))
                } else {
                    f(null)
                }
                val json = objectMapper().writeValueAsString(oppgave)

                tx.run(
                    queryOf(
                        """
                    insert into oppgave as k (id, data)
                    values (:id, :data :: jsonb)
                    on conflict (id) do update
                    set data =  :data :: jsonb
                 """, mapOf("id" to uuid.toString(), "data" to json)
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
                    "select data from oppgave " +
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
        val list =
            json.filter { it.indexOf("oppgaver") == -1 }.map { s -> objectMapper().readValue(s, Oppgave::class.java) }
                .toList()
                .sortedBy { oppgave -> oppgave.behandlingOpprettet }

        log.info("Henter oppgaver: " + list.size + " oppgaver" + " serialisering: " + (System.currentTimeMillis() - serialisering) + " spørring: " + spørring)
        return list
    }

    fun hentAlleOppgaverUnderArbeid(): List<AlleOppgaverDto> {
        try {
            val json = using(sessionOf(dataSource)) {
                it.run(
                    queryOf(
                        """
                        select count(*) as antall,
                        (data -> 'fagsakYtelseType' ->> 'kode') as fagsakYtelseType,
                        (data -> 'behandlingType' ->> 'kode') as behandlingType,
                        not (data -> 'tilBeslutter') ::boolean as tilBehandling
                        from oppgave o where (data -> 'aktiv') ::boolean
                        group by  behandlingType, fagsakYtelseType, tilBehandling
                    """.trimIndent(),
                        mapOf()
                    )
                        .map { row ->
                            AlleOppgaverDto(
                                FagsakYtelseType.fraKode(row.string("fagsakYtelseType")),
                                BehandlingType.fraKode(row.string("behandlingType")),
                                row.boolean("tilBehandling"),
                                row.int("antall")
                            )
                        }.asList
                )
            }
            return json
        } catch (_: Exception) {

            return emptyList()
        }
    }

    fun hentAlleOppgaverPerDato(): List<AlleOppgaverPerDato> {

        val json = using(sessionOf(dataSource)) {
            it.run(
                queryOf(
                    """
                        select count(*) as antall,
                        (data ::jsonb -> 'fagsakYtelseType' ->> 'kode') as fagsakYtelseType,
                        (data ::jsonb -> 'behandlingType' ->> 'kode') as behandlingType,
                        (data ::jsonb ->> 'behandlingOpprettet') ::date as opprettetDato
                        from oppgave o where (data ::jsonb -> 'aktiv') ::boolean
                        and (data ::jsonb ->> 'behandlingOpprettet')::date <= current_date 
                        and (data ::jsonb ->> 'behandlingOpprettet')::date >= (current_date - '28 days' ::interval)
                        group by opprettetDato, behandlingType, fagsakYtelseType
                    """.trimIndent(),
                    mapOf()
                )
                    .map { row ->
                        AlleOppgaverPerDato(
                            FagsakYtelseType.fraKode(row.string("fagsakYtelseType")),
                            BehandlingType.fraKode(row.string("behandlingType")),
                            row.localDate("opprettetDato"),
                            row.int("antall")
                        )
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
                    "select data from oppgave where data ->> 'aktorId' = :aktorId",
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
                    "select data from oppgave where lower(data ->> 'fagsakSaksnummer') = lower(:saksnummer)",
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
                    "select count(*) as count from oppgave where (data -> 'aktiv') ::boolean",
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
                    "select count(*) as count from oppgave where not (data -> 'fagsakYtelseType' ->> 'kode' = 'FRISINN')  and (data -> 'behandlingStatus' ->> 'kode' = 'AVSLU') ::boolean",
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
                    "select count(*) as count from oppgave o left join reservasjon r using (id) where not (o.data -> 'fagsakYtelseType' ->> 'kode' = 'FRISINN')  and (o.data ->'behandlingStatus' ->> 'kode' = 'AVSLU') and r.id is null",
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
                    "select data from oppgave where (data -> 'aktiv') ::boolean ",
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

    internal fun hentAktiveOppgaversAksjonspunktliste(): List<Aksjonspunkt> {
    
        val json: List<List<Aksjonspunkt>> = using(sessionOf(dataSource)) {
            it.run(
                queryOf(
                    "select (data -> 'aksjonspunkter' -> 'liste') punkt,  count(*) from oppgave where (data -> 'aktiv') ::boolean group by data -> 'aksjonspunkter' -> 'liste' ",
                    mapOf()
                )
                    .map { row ->
                        
                        val map =  objectMapper().readValue(
                            row.string("punkt"),
                            object : TypeReference<HashMap<String, String>>() {})
                        val antall = row.int("count")
                        val aksjonspunkter = map.keys.map { AksjonspunktDefinisjon.fraKode(it) }
                            .map { Aksjonspunkt(it.kode, it.navn, it.aksjonspunktType.navn,it.behandlingSteg.navn, "", "", it.defaultTotrinnBehandling, antall = antall ) }.toList()
                        aksjonspunkter
                    }.asList
            )
        }
       
        return json.flatten().groupBy { it.kode }.map { entry ->
            val aksjonspunkt = entry.value.get(0)
            aksjonspunkt.antall=  entry.value.map { it.antall }.sum()
            aksjonspunkt
        }
    }
    
}
