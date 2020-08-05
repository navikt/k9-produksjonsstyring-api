package no.nav.k9.domene.repository

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.k9.tjenester.admin.Driftsmelding
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource

class DriftsmeldingRepository(
    private val dataSource: DataSource
) {
    fun lagreDriftsmelding(driftsmelding: Driftsmelding) {
        using(sessionOf(dataSource)) {
            it.transaction { tx ->

                //language=PostgreSQL
                tx.run(
                    queryOf(
                        """
                    insert into driftsmeldinger as k (id, dato, melding, aktiv)
                    values (:id, :dato, :melding, :aktiv)                 
                       
                 """,
                        mapOf(
                            "id" to driftsmelding.id,
                            "dato" to driftsmelding.dato,
                            "melding" to driftsmelding.melding,
                            "aktiv" to driftsmelding.aktiv
                        )
                    ).asUpdate
                )
            }
        }
    }

    fun hentAlle(): List<Driftsmelding> {
        return using(sessionOf(dataSource)) {
            //language=PostgreSQL
            it.run(
                queryOf(
                    """select * from driftsmeldinger""".trimIndent()
                )
                    .map { row ->
                        Driftsmelding(
                            id = UUID.fromString(row.string("id")),
                            melding = row.string("melding"),
                            aktiv = row.boolean("aktiv"),
                            dato = row.localDateTime("dato")
                        )
                    }.asList
            )
        }
    }

    fun slett(id: UUID){
        using(sessionOf(dataSource)) {
            it.transaction { tx ->

                //language=PostgreSQL
                tx.run(
                    queryOf(
                        """
                    delete from driftsmeldinger where id = :id            
                 """,
                        mapOf(
                            "id" to id.toString()                           
                        )
                    ).asUpdate
                )
            }
        }
    }

}
