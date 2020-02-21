package no.nav.k9.aksjonspunktbehandling

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import no.nav.helse.dusseldorf.ktor.jackson.dusseldorfConfigured
import no.nav.k9.db.runMigration
import no.nav.k9.domene.repository.BehandlingProsessEventRepository
import no.nav.k9.domene.repository.OppgaveRepository
import no.nav.vedtak.felles.integrasjon.kafka.BehandlingProsessEventDto
import org.intellij.lang.annotations.Language
import org.junit.Assert
import org.junit.Test
import java.util.*
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertTrue


class K9sakEventHandlerTest {

    @Test
    fun `Skal lukke oppgave dersom den ikke har noen aktive aksjonspunkter`() {
        val pg = EmbeddedPostgres.start()
        val dataSource = pg.postgresDatabase
        runMigration(dataSource)
        val oppgaveRepository = OppgaveRepository(dataSource = dataSource)
        val k9sakEventHandler = K9sakEventHandler(
            oppgaveRepository,
            BehandlingProsessEventRepository(dataSource = dataSource)
        )

        val json =
            """{
                  "eksternId": "e84300c6-8976-46fa-8a68-9c7ac27ee636",
                  "fagsystem": "FPSAK",
                  "saksnummer": "5YC7C",
                  "aktørId": "9916108039470",
                  "behandlingId": 1000001,
                  "eventTid": null,
                  "eventHendelse": "BEHANDLINGSKONTROLL_EVENT",
                  "behandlinStatus": "UTRED",
                  "behandlingStatus": null,
                  "behandlingSteg": "INREG",
                  "behandlendeEnhet": "0300",
                  "ytelseTypeKode": "PSB",
                  "behandlingTypeKode": "BT-002",
                  "opprettetBehandling": "2020-02-19T08:31:56",
                  "aksjonspunktKoderMedStatusListe": {}
                }"""
        val objectMapper = jacksonObjectMapper()
            .dusseldorfConfigured().setPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CAMEL_CASE)

        val event = objectMapper.readValue(json, BehandlingProsessEventDto::class.java)

        k9sakEventHandler.prosesser(event)
        val oppgave = oppgaveRepository.hentOppgave(UUID.fromString("e84300c6-8976-46fa-8a68-9c7ac27ee636"))
        assertFalse { oppgave.aktiv }
    }

    @Test
    fun `Skal lukke oppgave dersom den er satt på vent`() {
        val pg = EmbeddedPostgres.start()
        val dataSource = pg.postgresDatabase
        runMigration(dataSource)
        val k9sakEventHandler = K9sakEventHandler(
            OppgaveRepository(dataSource = dataSource),
            BehandlingProsessEventRepository(dataSource = dataSource)
        )

        @Language("JSON") val json =
            """{
  "eksternId": "6b521f78-ef71-43c3-a615-6c2b8bb4dcdb",
  "fagsystem": "FPSAK",
  "saksnummer": "5YC4K",
  "aktørId": "9906098522415",
  "behandlingId": 1000001,
  "eventTid": null,
  "eventHendelse": "BEHANDLINGSKONTROLL_EVENT",
  "behandlinStatus": "UTRED",
  "behandlingStatus": null,
  "behandlingSteg": "INREG_AVSL",
  "behandlendeEnhet": "0300",
  "ytelseTypeKode": "PSB",
  "behandlingTypeKode": "BT-002",
  "opprettetBehandling": "2020-02-20T07:38:49",
  "aksjonspunktKoderMedStatusListe": {
    "7030": "OPPR"
  }
}"""
        val objectMapper = jacksonObjectMapper()
            .dusseldorfConfigured().setPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CAMEL_CASE)

        val event = objectMapper.readValue(json, BehandlingProsessEventDto::class.java)

        k9sakEventHandler.prosesser(event)

    }

}