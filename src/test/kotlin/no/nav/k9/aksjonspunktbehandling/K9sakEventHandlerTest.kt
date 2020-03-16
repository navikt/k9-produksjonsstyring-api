package no.nav.k9.aksjonspunktbehandling

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import io.ktor.util.KtorExperimentalAPI
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import no.nav.helse.dusseldorf.ktor.jackson.dusseldorfConfigured
import no.nav.k9.Configuration
import no.nav.k9.db.runMigration
import no.nav.k9.domene.repository.BehandlingProsessEventRepository
import no.nav.k9.domene.repository.OppgaveRepository
import no.nav.k9.integrasjon.gosys.GosysOppgave
import no.nav.k9.integrasjon.gosys.GosysOppgaveGateway
import no.nav.k9.kafka.dto.BehandlingProsessEventDto
import org.intellij.lang.annotations.Language
import org.junit.Test
import java.util.*
import kotlin.test.assertFalse
import kotlin.test.assertTrue


class K9sakEventHandlerTest {

    @KtorExperimentalAPI
    @Test
    fun `Skal lukke oppgave dersom den ikke har noen aktive aksjonspunkter`() {
        val pg = EmbeddedPostgres.start()
        val dataSource = pg.postgresDatabase
        runMigration(dataSource)
        val oppgaveRepository = OppgaveRepository(dataSource = dataSource)

        val gosysOppgaveGateway = mockk<GosysOppgaveGateway>()
        every { gosysOppgaveGateway.hentOppgaver(any()) } returns mutableListOf(GosysOppgave(1,1))
        every { gosysOppgaveGateway.avsluttOppgave(any()) } just Runs

        val k9sakEventHandler = K9sakEventHandler(
            oppgaveRepository,
            BehandlingProsessEventRepository(dataSource = dataSource),
//            gosysOppgaveGateway = gosysOppgaveGateway
            config = mockk<Configuration>()
        )

        val json =
            """{
                  "eksternId": "e84300c6-8976-46fa-8a68-9c7ac27ee636",
                  "fagsystem": "FPSAK",
                  "saksnummer": "5YC7C",
                  "aktørId": "9916108039470",
                  "behandlingId": 1000001,
                  "eventTid": "2020-02-20T07:38:49",
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
        val oppgaveModell = oppgaveRepository.hent(UUID.fromString("e84300c6-8976-46fa-8a68-9c7ac27ee636"))
        val oppgave = oppgaveModell.sisteOppgave()
        assertFalse { oppgave.aktiv }
    }

    @KtorExperimentalAPI
    @Test
    fun `Skal lukke oppgave dersom den er satt på vent`() {
        val pg = EmbeddedPostgres.start()
        val dataSource = pg.postgresDatabase
        runMigration(dataSource)
        val gosysOppgaveGateway = mockk<GosysOppgaveGateway>()
        every { gosysOppgaveGateway.hentOppgaver(any()) } returns mutableListOf(GosysOppgave(1,1))
        every { gosysOppgaveGateway.avsluttOppgave(any()) } just Runs

        val k9sakEventHandler = K9sakEventHandler(
            OppgaveRepository(dataSource = dataSource),
            BehandlingProsessEventRepository(dataSource = dataSource),
//            gosysOppgaveGateway = gosysOppgaveGateway
                    config = mockk<Configuration>()
        )

        @Language("JSON") val json =
            """{
  "eksternId": "6b521f78-ef71-43c3-a615-6c2b8bb4dcdb",
  "fagsystem": "FPSAK",
  "saksnummer": "5YC4K",
  "aktørId": "9906098522415",
  "behandlingId": 1000001,
  "eventTid": "2020-02-20T07:38:49",
  "eventHendelse": "BEHANDLINGSKONTROLL_EVENT",
  "behandlinStatus": "UTRED",
  "behandlingStatus": null,
  "behandlingSteg": "INREG_AVSL",
  "behandlendeEnhet": "0300",
  "ytelseTypeKode": "SVP",
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

    @KtorExperimentalAPI
    @Test
    fun `Skal opprette oppgave dersom 5009`() {
        val pg = EmbeddedPostgres.start()
        val dataSource = pg.postgresDatabase
        runMigration(dataSource)
        val gosysOppgaveGateway = mockk<GosysOppgaveGateway>()
        every { gosysOppgaveGateway.hentOppgaver(any()) } returns mutableListOf(GosysOppgave(1,1))
        every { gosysOppgaveGateway.avsluttOppgave(any()) } just Runs

        val oppgaveRepository = OppgaveRepository(dataSource = dataSource)
        val k9sakEventHandler = K9sakEventHandler(
            oppgaveRepository,
            BehandlingProsessEventRepository(dataSource = dataSource),
//            gosysOppgaveGateway = gosysOppgaveGateway
            config = mockk<Configuration>()
        )

        @Language("JSON") val json =
            """{
                  "eksternId": "6b521f78-ef71-43c3-a615-6c2b8bb4dcdb",
                  "fagsystem": "FPSAK",
                  "saksnummer": "5YC4K",
                  "aktørId": "9906098522415",
                  "behandlingId": 1000001,
                  "eventTid": "2020-02-20T07:38:49",
                  "eventHendelse": "BEHANDLINGSKONTROLL_EVENT",
                  "behandlinStatus": "UTRED",
                  "behandlingStatus": null,
                  "behandlingSteg": "INREG_AVSL",
                  "behandlendeEnhet": "0300",
                  "ytelseTypeKode": "SVP",
                  "behandlingTypeKode": "BT-002",
                  "opprettetBehandling": "2020-02-20T07:38:49",
                  "aksjonspunktKoderMedStatusListe": {
                    "5009": "OPPR"
                  }
                }"""
        val objectMapper = jacksonObjectMapper()
            .dusseldorfConfigured().setPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CAMEL_CASE)

        val event = objectMapper.readValue(json, BehandlingProsessEventDto::class.java)

        k9sakEventHandler.prosesser(event)
        val oppgave =
            oppgaveRepository.hent(UUID.fromString("6b521f78-ef71-43c3-a615-6c2b8bb4dcdb")).sisteOppgave()
        assertTrue { oppgave.aktiv }
    }

    @KtorExperimentalAPI
    @Test
    fun `Skal ha 1 oppgave med 3 aksjonspunkter`() {
        val pg = EmbeddedPostgres.start()
        val dataSource = pg.postgresDatabase
        runMigration(dataSource)
        val gosysOppgaveGateway = mockk<GosysOppgaveGateway>()
        every { gosysOppgaveGateway.hentOppgaver(any()) } returns mutableListOf(GosysOppgave(1,2))
        every { gosysOppgaveGateway.opprettOppgave(any()) } returns GosysOppgave(1,3)

        val oppgaveRepository = OppgaveRepository(dataSource = dataSource)
        val k9sakEventHandler = K9sakEventHandler(
            oppgaveRepository,
            BehandlingProsessEventRepository(dataSource = dataSource),
            config = mockk<Configuration>()
//            gosysOppgaveGateway = gosysOppgaveGateway
        )

        @Language("JSON") val json =
            """{
                  "eksternId": "6b521f78-ef71-43c3-a615-6c2b8bb4dcdb",
                  "fagsystem": "FPSAK",
                  "saksnummer": "5YC4K",
                  "aktørId": "9906098522415",
                  "behandlingId": 1000001,
                  "eventTid": "2020-02-20T07:38:49",
                  "eventHendelse": "BEHANDLINGSKONTROLL_EVENT",
                  "behandlinStatus": "UTRED",
                  "behandlingStatus": null,
                  "behandlingSteg": "INREG_AVSL",
                  "behandlendeEnhet": "0300",
                  "ytelseTypeKode": "SVP",
                  "behandlingTypeKode": "BT-002",
                  "opprettetBehandling": "2020-02-20T07:38:49",
                  "aksjonspunktKoderMedStatusListe": {
                    "5009": "OPPR",
                    "5084": "OPPR",
                    "5085": "OPPR"
                  }
                }"""
        val objectMapper = jacksonObjectMapper()
            .dusseldorfConfigured().setPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CAMEL_CASE)

        val event = objectMapper.readValue(json, BehandlingProsessEventDto::class.java)

        k9sakEventHandler.prosesser(event)
        val oppgave =
            oppgaveRepository.hent(UUID.fromString("6b521f78-ef71-43c3-a615-6c2b8bb4dcdb")).sisteOppgave()
        assertTrue { oppgave.aktiv }
        assertTrue(oppgave.aksjonspunkter.lengde() == 3)
    }


    fun startBehandling() {
        //language=JSON
        """{
              "eksternId": "bc0636ee-b26c-4155-a787-95a82392944c",
              "fagsystem": "K9SAK",
              "saksnummer": "5YC4K",
              "aktørId": "9929038413668",
              "behandlingId": 1000002,
              "eventTid": "2020-03-16T10:38:03.017816",
              "eventHendelse": "BEHANDLINGSKONTROLL_EVENT",
              "behandlinStatus": "UTRED",
              "behandlingStatus": null,
              "behandlingSteg": "INREG",
              "behandlendeEnhet": "4833",
              "ytelseTypeKode": "PSB",
              "behandlingTypeKode": "BT-002",
              "opprettetBehandling": "2020-03-16T10:38",
              "aksjonspunktKoderMedStatusListe": {}
            }
          """

    }

    fun ventPåEtterlystInntektsmelding() {
        //language=JSON
        """{
              "eksternId": "bc0636ee-b26c-4155-a787-95a82392944c",
              "fagsystem": "K9SAK",
              "saksnummer": "5YC4K",
              "aktørId": "9929038413668",
              "behandlingId": 1000002,
              "eventTid": "2020-03-16T10:38:07.439524",
              "eventHendelse": "BEHANDLINGSKONTROLL_EVENT",
              "behandlinStatus": "UTRED",
              "behandlingStatus": null,
              "behandlingSteg": "INREG_AVSL",
              "behandlendeEnhet": "4833",
              "ytelseTypeKode": "PSB",
              "behandlingTypeKode": "BT-002",
              "opprettetBehandling": "2020-03-16T10:38",
              "aksjonspunktKoderMedStatusListe": {
                "7030": "OPPR"
              }
            }
        """

    }

    fun startBehandling3() {
        //language=JSON
        """{
              "eksternId": "bc0636ee-b26c-4155-a787-95a82392944c",
              "fagsystem": "K9SAK",
              "saksnummer": "5YC4K",
              "aktørId": "9929038413668",
              "behandlingId": 1000002,
              "eventTid": "2020-03-16T10:38:11.134276",
              "eventHendelse": "BEHANDLINGSKONTROLL_EVENT",
              "behandlinStatus": "UTRED",
              "behandlingStatus": null,
              "behandlingSteg": "VURDERMV",
              "behandlendeEnhet": "4833",
              "ytelseTypeKode": "PSB",
              "behandlingTypeKode": "BT-002",
              "opprettetBehandling": "2020-03-16T10:38",
              "aksjonspunktKoderMedStatusListe": {
                "5053": "OPPR",
                "9001": "OPPR",
                "7030": "UTFO"
              }
            }
        """

    }

    fun startBehandling4() {
        //language=JSON
        """{
              "eksternId": "bc0636ee-b26c-4155-a787-95a82392944c",
              "fagsystem": "K9SAK",
              "saksnummer": "5YC4K",
              "aktørId": "9929038413668",
              "behandlingId": 1000002,
              "eventTid": "2020-03-16T10:38:13.843146",
              "eventHendelse": "BEHANDLINGSKONTROLL_EVENT",
              "behandlinStatus": "UTRED",
              "behandlingStatus": null,
              "behandlingSteg": "VURDER_MEDISINSK",
              "behandlendeEnhet": "4833",
              "ytelseTypeKode": "PSB",
              "behandlingTypeKode": "BT-002",
              "opprettetBehandling": "2020-03-16T10:38",
              "aksjonspunktKoderMedStatusListe": {
                "5053": "UTFO",
                "9001": "OPPR",
                "7030": "UTFO"
              }
            }
        """

    }
}