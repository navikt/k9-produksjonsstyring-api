package no.nav.k9.domene.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.zaxxer.hikari.HikariDataSource
import no.nav.k9.integrasjon.BehandlingK9sak
import no.nav.vedtak.felles.integrasjon.kafka.BehandlingProsessEventDto
import java.util.*


class BehandlingProsessEventRepository(private val dataSource: HikariDataSource) {
    fun behandlingProsessEventer(uuid: UUID): BehandlingProsessEventer {
        val SQL_QUERY = "select data from behandling_prosess_events_k9 where data -> 'uuid' == ? order by id desc"

        dataSource.connection.use { con ->
            con.prepareStatement(SQL_QUERY).use { pst ->
                pst.setString(0, uuid.toString())
                pst.executeQuery().use { rs ->
                    while (rs.next()) {
                        return ObjectMapper().readValue(rs.getString("data"), BehandlingProsessEventer::class.java)
                    }
                }
            }
        }
        throw IllegalArgumentException()
    }

    fun lagreBehandlingProsessEvent(
        event: BehandlingProsessEventDto,
        behandling: BehandlingK9sak
    ): BehandlingProsessEventer {
        // Legge service i mellom og mappe fra event til returverdi


        // Ta lås
        val SQL_QUERY = """
            insert into behandling_prosess_events_k9
            values (id = ?, data = ?)
            on conflict (id) do update
                set data = data -> 'oppgaver' || ? :: jsonb"""
        val eventJson = ObjectMapper().writeValueAsString(event)
        dataSource.connection.use { con ->
            con.prepareStatement(SQL_QUERY).use { pst ->
                pst.setString(0, event.eksternId.toString())
                pst.setString(1, eventJson)
                pst.setString(2, eventJson)
                pst.executeQuery()
            }
        }
        return behandlingProsessEventer(event.eksternId)
    }

}