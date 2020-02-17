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
                            ObjectMapper().readValue(rs.getString("data"), OppgaveEventLogg::class.java)
                        )
                    }
                }
            }
        }
        return eventer
    }


    fun hentOppgave(eksternId: UUID): Oppgave {
        val SQL_QUERY = "select data -> (json_array_length(data ->'items')-1) from oppgave where id == ?"
        val oppgaver: MutableList<Oppgave> = ArrayList()
        dataSource.connection.use { con ->
            con.prepareStatement(SQL_QUERY).use { pst ->
                pst.setString(0, eksternId.toString())
                pst.executeQuery().use { rs ->
                    while (rs.next()) {
                        oppgaver.add(
                            ObjectMapper().readValue(rs.getString("data"), Oppgave::class.java)
                        )
                    }
                }
            }
        }
        return oppgaver[oppgaver.lastIndex]
    }

    fun opprettEllerEndreOppgave(oppgave: Oppgave) {
        val SQL_QUERY = """
            insert into oppgave
            values (id = ?, data = ?)
            on conflict (id) do update
                set data = data || ? :: jsonb"""
        dataSource.connection.use { con ->
            con.prepareStatement(SQL_QUERY).use { pst ->
                pst.setString(0, oppgave.eksternId.toString())
                pst.setString(1, ObjectMapper().writeValueAsString(oppgave))
                pst.setString(2, ObjectMapper().writeValueAsString(oppgave))
                pst.executeQuery()
            }
        }
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

    fun opprettOppgave(oppgave: Oppgave) {
        val SQL_QUERY = "insert into oppgave values (data = ?)"
        dataSource.connection.use { con ->
            con.prepareStatement(SQL_QUERY).use { pst ->
                pst.setString(0, ObjectMapper().writeValueAsString(oppgave))
                pst.executeQuery()
            }
        }
    }


}