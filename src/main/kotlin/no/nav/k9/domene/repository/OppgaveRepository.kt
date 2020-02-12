package no.nav.k9.domene.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.zaxxer.hikari.HikariDataSource
import no.nav.k9.domene.lager.oppgave.Oppgave
import no.nav.k9.domene.lager.oppgave.OppgaveEventLogg
import java.util.*


class OppgaveRepository(private val dataSource: HikariDataSource) {
    fun hentEventer(uuid: UUID): List<OppgaveEventLogg> {
        val SQL_QUERY = "select data from oppgave_event_logg where data -> 'uuid' == ? order by id desc"
        val eventer: MutableList<OppgaveEventLogg> = ArrayList()
        dataSource.connection.use { con ->
            con.prepareStatement(SQL_QUERY).use { pst ->
                pst.setString(0, uuid.toString())
                pst.executeQuery().use { rs ->
                    while (rs.next()) {
                        eventer.add(
                            ObjectMapper().readValue(rs.getString("data"), OppgaveEventLogg::class.java))
                    }
                }
            }
        }
        return eventer
    }

    fun avsluttOppgave(behandlingId: Long) {
       TODO("Not yet implemented")
    }

    fun lagre(oppgaveEventLogg: OppgaveEventLogg) {
        val SQL_QUERY = "insert into oppgave_event_logg values (data = ?)"
        dataSource.connection.use { con ->
            con.prepareStatement(SQL_QUERY).use { pst ->
                pst.setString(0, ObjectMapper().writeValueAsString(oppgaveEventLogg))
                pst.executeQuery()
            }
        }
    }

    fun gjenåpneOppgave(eksternId: UUID): Oppgave {
        TODO("Not yet implemented")
    }
}