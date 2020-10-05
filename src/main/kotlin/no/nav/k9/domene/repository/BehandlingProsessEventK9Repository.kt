package no.nav.k9.domene.repository

import io.ktor.util.*
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.k9.aksjonspunktbehandling.objectMapper
import no.nav.k9.domene.modell.K9SakModell
import no.nav.k9.integrasjon.kafka.dto.BehandlingProsessEventDto
import no.nav.k9.tjenester.innsikt.Databasekall
import no.nav.k9.tjenester.innsikt.Mapping
import java.util.*
import java.util.concurrent.atomic.LongAdder
import javax.sql.DataSource


class BehandlingProsessEventK9Repository(private val dataSource: DataSource) {
   
    fun hent(uuid: UUID): K9SakModell {
        val json: String? = using(sessionOf(dataSource)) {
            it.run(
                queryOf(
                    "select data from behandling_prosess_events_k9 where id = :id",
                    mapOf("id" to uuid.toString())
                )
                    .map { row ->
                        row.string("data")
                    }.asSingle
            )
        }
        Databasekall.map.computeIfAbsent(object{}.javaClass.name + object{}.javaClass.enclosingMethod.name){LongAdder()}.increment()
        if (json.isNullOrEmpty()) {
            return K9SakModell(emptyList())
        }
        val modell = objectMapper().readValue(json, K9SakModell::class.java)
     
        return K9SakModell(  modell.eventer.sortedBy { it.eventTid })
    }

    fun lagre(
        event: BehandlingProsessEventDto
    ): K9SakModell {
        val json = objectMapper().writeValueAsString(event)

        val id = event.eksternId.toString()
        val out = using(sessionOf(dataSource)) {
            it.transaction { tx ->
                tx.run(
                    queryOf(
                        """
                    insert into behandling_prosess_events_k9 as k (id, data)
                    values (:id, :dataInitial :: jsonb)
                    on conflict (id) do update
                    set data = jsonb_set(k.data, '{eventer,999999}', :data :: jsonb, true)
                 """, mapOf("id" to id, "dataInitial" to "{\"eventer\": [$json]}", "data" to json)
                    ).asUpdate
                )
                tx.run(
                    queryOf(
                        "select data from behandling_prosess_events_k9 where id = :id",
                        mapOf("id" to id)
                    )
                        .map { row ->
                            row.string("data")
                        }.asSingle
                )
            }

        }
        Databasekall.map.computeIfAbsent(object{}.javaClass.name + object{}.javaClass.enclosingMethod.name){LongAdder()}.increment()
        return objectMapper().readValue(out!!, K9SakModell::class.java)

    }

    fun hentAlleEventerIder(
    ): List<String> {

        val ider = using(sessionOf(dataSource)) {
            it.transaction { tx ->
                tx.run(
                    queryOf(
                        "select id from behandling_prosess_events_k9",
                        mapOf()
                    )
                        .map { row ->
                            row.string("id")
                   }.asList
                )
            }

        }
        return ider

    }

    fun eldsteEventTid(): String {
        val json: String? = using(sessionOf(dataSource)) {
            //language=PostgreSQL
            it.run(
                queryOf(
                    """select sort_array(data->'eventer', 'eventTid') -> 0 ->'eventTid' as data from behandling_prosess_events_k9 order by (sort_array(data->'eventer', 'eventTid') -> 0 ->'eventTid') limit 1;""",
                    mapOf()
                )
                    .map { row ->
                        row.string("data")
                    }.asSingle
            )
        }
        Databasekall.map.computeIfAbsent(object{}.javaClass.name + object{}.javaClass.enclosingMethod.name){LongAdder()}.increment()
        return  json!!
    }
    
    fun mapMellomeksternIdOgBehandlingsid(): List<Mapping> {
        Databasekall.map.computeIfAbsent(object{}.javaClass.name + object{}.javaClass.enclosingMethod.name){LongAdder()}.increment()
        return using(sessionOf(dataSource)) {
            //language=PostgreSQL
            it.run(
                queryOf(
                    """select id, (data-> 'eventer' -> -1 ->'behandlingId' ) as behandlingid from behandling_prosess_events_k9""",
                    mapOf()
                )
                    .map { row ->
                        Mapping(id = row.string("behandlingid"), uuid = row.string("id"))                        
                    }.asList
            )
        }
    }
}
