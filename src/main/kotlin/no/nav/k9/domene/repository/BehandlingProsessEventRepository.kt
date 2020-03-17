package no.nav.k9.domene.repository

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.k9.aksjonspunktbehandling.objectMapper
import no.nav.k9.domene.modell.Modell
import no.nav.k9.kafka.dto.BehandlingProsessEventDto
import java.util.*
import javax.sql.DataSource


class BehandlingProsessEventRepository(private val dataSource: DataSource) {
    fun hent(uuid: UUID): Modell {
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
        if (json.isNullOrEmpty()) {
            return Modell(emptyList())
        }
        return objectMapper().readValue(json, Modell::class.java)
    }

    fun lagre(
        event: BehandlingProsessEventDto
    ): Modell {
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
        return objectMapper().readValue(out!!, Modell::class.java)

    }

}
