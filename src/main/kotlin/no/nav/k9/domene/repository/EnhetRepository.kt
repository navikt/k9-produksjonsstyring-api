package no.nav.k9.domene.repository

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.k9.aksjonspunktbehandling.objectMapper
import no.nav.k9.domene.modell.Enhet
import javax.sql.DataSource
import kotliquery.using

class EnhetRepository(private val dataSource: DataSource) {

    fun hent(avdelingEnhet: String): Enhet {

        val json: String? = using(sessionOf(dataSource)) {
            it.run(
                queryOf("select data from enhet where id = :id", mapOf("id" to avdelingEnhet))
                    .map { row ->
                        row.string("data")
                    }.asSingle
            )
        }
        return objectMapper().readValue(json!!, Enhet::class.java)
    }

    fun lagre(enhet: Enhet) {
        val json = objectMapper().writeValueAsString(enhet)
        using(sessionOf(dataSource)) {
            it.run(
                queryOf(
                    """
                    insert into enhet as k (id, data)
                    values (:id, :data :: jsonb)
                    on conflict (id) do update
                    set data = :data :: jsonb
                 """, mapOf("id" to enhet.avdelingEnhet, "data" to json)
                ).asUpdate
            )
        }
    }
}