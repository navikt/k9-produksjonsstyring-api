package no.nav.k9.domene.repository

import com.fasterxml.jackson.core.type.TypeReference
import io.ktor.util.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.k9.aksjonspunktbehandling.objectMapper
import no.nav.k9.domene.lager.oppgave.Oppgave
import no.nav.k9.domene.modell.BehandlingType
import no.nav.k9.domene.modell.FagsakYtelseType
import no.nav.k9.integrasjon.abac.IPepClient
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon
import no.nav.k9.tjenester.avdelingsleder.nokkeltall.AlleApneBehandlinger
import no.nav.k9.tjenester.avdelingsleder.nokkeltall.AlleOppgaverDto
import no.nav.k9.tjenester.mock.Aksjonspunkt
import no.nav.k9.utils.Cache
import no.nav.k9.utils.CacheObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import javax.sql.DataSource


class OppgaveRepository(
    private val dataSource: DataSource,
    private val pepClient: IPepClient,
    private val refreshOppgave: Channel<Oppgave>
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

    @KtorExperimentalAPI
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
                runBlocking {
                    oppgave.kode6 = pepClient.erSakKode6(oppgave.fagsakSaksnummer)
                    oppgave.skjermet = pepClient.erSakKode7EllerEgenAnsatt(oppgave.fagsakSaksnummer)
                }
                val json = objectMapper().writeValueAsString(oppgave)
                tx.run(
                    queryOf(
                        """
                    insert into oppgave as k (id, data)
                    values (:id, :data :: jsonb)
                    on conflict (id) do update
                    set data = :data :: jsonb
                 """, mapOf("id" to uuid.toString(), "data" to json)
                    ).asUpdate
                )
            }
        }
    }


    @KtorExperimentalAPI
    fun hentOppgaver(oppgaveider: Collection<UUID>): List<Oppgave> {

        val oppgaveiderList = oppgaveider.toList()
        if (oppgaveider.isEmpty()) {
            return emptyList()
        }

        val session = sessionOf(dataSource)
        val json: List<String> = using(session) {

            //language=PostgreSQL
            it.run(
                queryOf(
                    "select data from oppgave " +
                            "where id in (${
                                IntRange(0, oppgaveiderList.size - 1).map { t -> ":p$t" }.joinToString()
                            })",
                    IntRange(0, oppgaveiderList.size - 1).map { t -> "p$t" to oppgaveiderList[t].toString() as Any }
                        .toMap()
                )
                    .map { row ->
                        row.string("data")
                    }.asList
            )
        }

        return json.filter { it.indexOf("oppgaver") == -1 }
            .map { s -> objectMapper().readValue(s, Oppgave::class.java) }
            .toList()
            .sortedBy { oppgave -> oppgave.behandlingOpprettet }
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
        } catch (e: Exception) {
            log.error("", e)
            return emptyList()
        }
    }

    fun hentApneBehandlingerPerBehandlingtypeIdag(): List<AlleApneBehandlinger> {
        try {
            val json = using(sessionOf(dataSource)) {
                it.run(
                    queryOf(
                        """
                        select count(*) as antall,
                        (data -> 'behandlingType' ->> 'kode') as behandlingType
                        from oppgave o where (data -> 'aktiv') ::boolean
                        group by  behandlingType
                    """.trimIndent(),
                        mapOf()
                    )
                        .map { row ->
                            AlleApneBehandlinger(
                                BehandlingType.fraKode(row.string("behandlingType")),
                                row.int("antall")
                            )
                        }.asList
                )
            }
            return json
        } catch (e: Exception) {
            log.error("", e)
            return emptyList()
        }
    }


    suspend fun hentOppgaverMedAktorId(aktørId: String): List<Oppgave> {
        val kode6 = pepClient.harTilgangTilKode6()
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
        val oppgaver =
            json.map { s -> objectMapper().readValue(s, Oppgave::class.java) }.filter { it.kode6 == kode6 }.toList()
        oppgaver.forEach { refreshOppgave.offer(it) }
        return oppgaver
    }

    @KtorExperimentalAPI
    suspend fun hentOppgaverMedSaksnummer(saksnummer: String): List<Oppgave> {
        val kode6 = pepClient.harTilgangTilKode6()
        val json: List<String> = using(sessionOf(dataSource)) {
            //language=PostgreSQL
            it.run(
                queryOf(
                    "select data from oppgave where lower(data ->> 'fagsakSaksnummer') = lower(:saksnummer) ",
                    mapOf("saksnummer" to saksnummer)
                )
                    .map { row ->
                        row.string("data")
                    }.asList
            )
        }
        val oppgaver = json.map { objectMapper().readValue(it, Oppgave::class.java) }.filter { it.kode6 == kode6 }
        oppgaver.forEach { refreshOppgave.offer(it) }
        return oppgaver
    }

    internal suspend fun hentAktiveOppgaverTotalt(): Int {
        val kode6 = pepClient.harTilgangTilKode6()
        var spørring = System.currentTimeMillis()
        val count: Int? = using(sessionOf(dataSource)) {
            it.run(
                queryOf(
                    "select count(*) as count from oppgave where (data -> 'aktiv') ::boolean and (data -> 'kode6'):: Boolean =:kode6 ",
                    mapOf("kode6" to kode6)
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

    internal  fun hentAktiveOppgaverTotaltIkkeSkjermede(): Int {
        val kode6 = false
        var spørring = System.currentTimeMillis()
        val count: Int? = using(sessionOf(dataSource)) {
            it.run(
                queryOf(
                    "select count(*) as count from oppgave where (data -> 'aktiv') ::boolean and (data -> 'kode6'):: Boolean =:kode6 ",
                    mapOf("kode6" to kode6)
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

    internal fun hentAktiveOppgaverTotaltPerBehandlingstypeOgYtelseType(
        fagsakYtelseType: FagsakYtelseType,
        behandlingType: BehandlingType
    ): Int {
        val count: Int? = using(sessionOf(dataSource)) {
            //language=PostgreSQL
            it.run(
                queryOf(
                    "select count(*) as count from oppgave where (data -> 'aktiv') ::boolean and (data -> 'behandlingType' ->> 'kode') =:behandlingType and (data -> 'fagsakYtelseType' ->> 'kode') =:fagsakYtelseType ",
                    mapOf("behandlingType" to behandlingType.kode, "fagsakYtelseType" to fagsakYtelseType.kode)
                )
                    .map { row ->
                        row.int("count")
                    }.asSingle
            )
        }
        return count!!
    }

    internal fun hentAvsluttede(): Int {
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

    internal fun hentInaktiveIkkeAvluttedeAvsluttede(): Int {
        var spørring = System.currentTimeMillis()
        val count: Int? = using(sessionOf(dataSource)) {
            it.run(
                queryOf(
                    "select count(*) as count from oppgave where not (data -> 'fagsakYtelseType' ->> 'kode' = 'FRISINN')  and (data -> 'behandlingStatus' ->> 'kode' != 'AVSLU') and (data -> 'aktiv')::boolean = false",
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
        log.info("Teller automatiske oppgaver: $spørring ms")
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
                    "select (data -> 'aksjonspunkter' -> 'liste') punkt,  count(*) from oppgave where (data -> 'aktiv') and (data -> 'system' = 'K9SAK')  ::boolean group by data -> 'aksjonspunkter' -> 'liste' ",
                    mapOf()
                )
                    .map { row ->

                        val map = objectMapper().readValue(
                            row.string("punkt"),
                            object : TypeReference<HashMap<String, String>>() {})
                        val antall = row.int("count")
                        val aksjonspunkter = map.keys.map { AksjonspunktDefinisjon.fraKode(it) }
                            .map {
                                Aksjonspunkt(
                                    it.kode,
                                    it.navn,
                                    it.aksjonspunktType.navn,
                                    it.behandlingSteg.navn,
                                    "",
                                    "",
                                    it.defaultTotrinnBehandling,
                                    antall = antall
                                )
                            }.toList()
                        aksjonspunkter
                    }.asList
            )
        }

        return json.flatten().groupBy { it.kode }.map { entry ->
            val aksjonspunkt = entry.value.get(0)
            aksjonspunkt.antall = entry.value.map { it.antall }.sum()
            aksjonspunkt
        }
    }

}
