package no.nav.k9.domene.repository

import no.nav.k9.aksjonspunktbehandling.objectMapper
import no.nav.k9.domene.lager.oppgave.Oppgave
import no.nav.k9.domene.lager.oppgave.OppgaveModell
import java.util.*
import javax.sql.DataSource


class OppgaveRepository(private val dataSource: DataSource) {

    fun hent(uuid: UUID): OppgaveModell {
        val SQL_QUERY = "select data from oppgave where id = ?"

        dataSource.connection.use { con ->
            con.prepareStatement(SQL_QUERY).use { pst ->
                pst.setString(1, uuid.toString())
                pst.executeQuery().use { rs ->
                    while (rs.next()) {
                        val string = rs.getString("data")
                        return objectMapper().readValue(string, OppgaveModell::class.java)
                    }
                }
            }
        }
        throw IllegalArgumentException()
    }

    fun lagre(oppgave: Oppgave) {
        val SQL_QUERY = """
            insert into oppgave as k (id, data)
            values (?, ? :: jsonb)
            on conflict (id) do update
            set data = jsonb_set(k.data, '{oppgaver,999999}', ? :: jsonb, true)
         """

        val eventJson = objectMapper().writeValueAsString(oppgave)
        dataSource.connection.use { con ->
            con.prepareStatement(SQL_QUERY).use { pst ->
                pst.setString(1, oppgave.eksternId.toString())
                pst.setString(2, "{\"oppgaver\": [$eventJson]}")
                pst.setString(3, eventJson)
                pst.executeUpdate()
            }
        }
    }
}