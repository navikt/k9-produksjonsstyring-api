package no.nav.k9.domene.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.zaxxer.hikari.HikariDataSource
import no.nav.k9.aksjonspunktbehandling.objectMapper
import no.nav.k9.integrasjon.BehandlingK9sak
import no.nav.vedtak.felles.integrasjon.kafka.BehandlingProsessEventDto
import java.util.*
import javax.sql.DataSource


class BehandlingProsessEventRepository(private val dataSource: DataSource) {
    fun behandlingProsessEventer(uuid: UUID): BehandlingProsessEventer {
        val SQL_QUERY = "select data from behandling_prosess_events_k9 where id = ?"

        dataSource.connection.use { con ->
            con.prepareStatement(SQL_QUERY).use { pst ->
                pst.setString(1, uuid.toString())
                pst.executeQuery().use { rs ->
                    while (rs.next()) {
                        val string = rs.getString("data")
                        return objectMapper().readValue(string, BehandlingProsessEventer::class.java)
                    }
                }
            }
        }
        throw IllegalArgumentException()
    }

    fun lagreBehandlingProsessEvent(
        event: BehandlingProsessEventDto
    ): BehandlingProsessEventer {
        // Legge service i mellom og mappe fra event til returverdi

        val SQL_QUERY = """
            insert into behandling_prosess_events_k9 as k (id, data)
            values (?, ? :: jsonb)
            on conflict (id) do update
            set data = jsonb_set(k.data, '{eventer,-1}', ? :: jsonb, true)
         """


        val eventJson = objectMapper().writeValueAsString(event)
        dataSource.connection.use { con ->
            con.prepareStatement(SQL_QUERY).use { pst ->
                pst.setString(1, event.eksternId.toString())
                pst.setString(2, "{\"eventer\": [$eventJson]}")
                pst.setString(3, eventJson)
                pst.executeUpdate()
            }
        }
        return behandlingProsessEventer(event.eksternId)
    }

}