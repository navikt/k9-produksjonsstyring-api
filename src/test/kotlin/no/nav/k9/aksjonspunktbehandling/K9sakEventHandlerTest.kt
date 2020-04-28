package no.nav.k9.aksjonspunktbehandling

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import io.ktor.util.KtorExperimentalAPI
import io.mockk.*
import no.nav.helse.dusseldorf.ktor.jackson.dusseldorfConfigured
import no.nav.k9.Configuration
import no.nav.k9.db.runMigration
import no.nav.k9.domene.lager.oppgave.Reservasjon
import no.nav.k9.domene.repository.BehandlingProsessEventRepository
import no.nav.k9.domene.repository.OppgaveRepository
import no.nav.k9.integrasjon.gosys.GosysOppgave
import no.nav.k9.integrasjon.gosys.GosysOppgaveGateway
import no.nav.k9.kafka.dto.BehandlingProsessEventDto
import no.nav.k9.sakogbehandling.SakOgBehadlingProducer
import org.intellij.lang.annotations.Language
import org.junit.Test
import java.time.LocalDateTime
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
        val sakOgBehadlingProducer = mockk<SakOgBehadlingProducer>()
        every { gosysOppgaveGateway.hentOppgaver(any()) } returns mutableListOf(GosysOppgave(1, 1))
        every { gosysOppgaveGateway.avsluttOppgave(any()) } just Runs
        every { sakOgBehadlingProducer.opprettetBehandlng(any(), any()) } just runs
        val config = mockk<Configuration>()
        val k9sakEventHandler = K9sakEventHandler(
            oppgaveRepository,
            BehandlingProsessEventRepository(dataSource = dataSource),
//            gosysOppgaveGateway = gosysOppgaveGateway
            config = config,
            sakOgBehadlingProducer = sakOgBehadlingProducer
        )

        @Language("JSON") val json =
            """{
                  "eksternId": "70c7a780-08ad-4ccf-8cef-c341d4913d65",
                  "fagsystem": {
                    "kode": "K9SAK",
                    "kodeverk": "FAGSYSTEM"
                  },
                  "saksnummer": "5YC1S",
                  "aktørId": "9916107629061",
                  "behandlingId": 999951,
                   "behandlingstidFrist": "2020-03-31",
                  "eventTid": "2020-03-31T06:33:59.460931",
                  "eventHendelse": "BEHANDLINGSKONTROLL_EVENT",
                  "behandlinStatus": "UTRED",
                  "behandlingStatus": null,
                  "behandlingSteg": "INREG",
                  "behandlendeEnhet": null,
                  "ansvarligBeslutterForTotrinn": null,
                  "ansvarligSaksbehandlerForTotrinn": null,
                  "ytelseTypeKode": "OMP",
                  "behandlingTypeKode": "BT-002",
                  "opprettetBehandling": "2020-03-31T06:33:48",
                  "aksjonspunktKoderMedStatusListe": {}
                }
            """.trimIndent()
        val objectMapper = jacksonObjectMapper()
            .dusseldorfConfigured().setPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CAMEL_CASE)

        val event = objectMapper.readValue(json, BehandlingProsessEventDto::class.java)

        k9sakEventHandler.prosesser(event)
        val oppgaveModell = oppgaveRepository.hent(UUID.fromString(event.eksternId.toString()))
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
        val sakOgBehadlingProducer = mockk<SakOgBehadlingProducer>()
        every { gosysOppgaveGateway.hentOppgaver(any()) } returns mutableListOf(GosysOppgave(1, 1))
        every { gosysOppgaveGateway.avsluttOppgave(any()) } just Runs
        every { sakOgBehadlingProducer.opprettetBehandlng(any(), any()) } just runs
        val config = mockk<Configuration>()
        val k9sakEventHandler = K9sakEventHandler(
            OppgaveRepository(dataSource = dataSource),
            BehandlingProsessEventRepository(dataSource = dataSource),
//            gosysOppgaveGateway = gosysOppgaveGateway
            config = config,
            sakOgBehadlingProducer = sakOgBehadlingProducer
        )

        @Language("JSON") val json =
            """{
              "eksternId": "6b521f78-ef71-43c3-a615-6c2b8bb4dcdb",
              "fagsystem": {
                "kode": "K9SAK",
                "kodeverk": "FAGSYSTEM"
              },
              "saksnummer": "5YC4K",
              "aktørId": "9906098522415",
              "behandlingId": 1000001,
              "eventTid": "2020-02-20T07:38:49",
              "eventHendelse": "BEHANDLINGSKONTROLL_EVENT",
              "behandlinStatus": "UTRED",
               "behandlingstidFrist": "2020-03-31",
              "behandlingStatus": "UTRED",
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

    @KtorExperimentalAPI
    @Test
    fun `Skal opprette oppgave dersom 5009`() {
        val pg = EmbeddedPostgres.start()
        val dataSource = pg.postgresDatabase
        runMigration(dataSource)
        val gosysOppgaveGateway = mockk<GosysOppgaveGateway>()
        val sakOgBehadlingProducer = mockk<SakOgBehadlingProducer>()
        every { gosysOppgaveGateway.hentOppgaver(any()) } returns mutableListOf(GosysOppgave(1, 1))
        every { gosysOppgaveGateway.avsluttOppgave(any()) } just Runs
        every { sakOgBehadlingProducer.opprettetBehandlng(any(), any()) } just runs
        val oppgaveRepository = OppgaveRepository(dataSource = dataSource)
        val config = mockk<Configuration>()
        val k9sakEventHandler = K9sakEventHandler(
            oppgaveRepository,
            BehandlingProsessEventRepository(dataSource = dataSource),
//            gosysOppgaveGateway = gosysOppgaveGateway
            config = config,
            sakOgBehadlingProducer = sakOgBehadlingProducer
        )

        @Language("JSON") val json =
            """{
                  "eksternId": "6b521f78-ef71-43c3-a615-6c2b8bb4dcdb",
                  "fagsystem": {
                    "kode": "K9SAK",
                    "kodeverk": "FAGSYSTEM"
                  },
                  "saksnummer": "5YC4K",
                  "aktørId": "9906098522415",
                  "behandlingId": 1000001,
                  "eventTid": "2020-02-20T07:38:49",
                  "eventHendelse": "BEHANDLINGSKONTROLL_EVENT",
                  "behandlinStatus": "UTRED",
                   "behandlingstidFrist": "2020-03-31",
                  "behandlingStatus": "UTRED",
                  "behandlingSteg": "INREG_AVSL",
                  "behandlendeEnhet": "0300",
                  "ytelseTypeKode": "OMP",
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
        val sakOgBehadlingProducer = mockk<SakOgBehadlingProducer>()
        every { gosysOppgaveGateway.hentOppgaver(any()) } returns mutableListOf(GosysOppgave(1, 2))
        every { gosysOppgaveGateway.opprettOppgave(any()) } returns GosysOppgave(1, 3)
        every { sakOgBehadlingProducer.opprettetBehandlng(any(), any()) } just runs

        val oppgaveRepository = OppgaveRepository(dataSource = dataSource)
        val config = mockk<Configuration>()
        val k9sakEventHandler = K9sakEventHandler(
            oppgaveRepository,
            BehandlingProsessEventRepository(dataSource = dataSource),
            config = config,
            sakOgBehadlingProducer = sakOgBehadlingProducer
        )

        @Language("JSON") val json =
            """{
                  "eksternId": "6b521f78-ef71-43c3-a615-6c2b8bb4dcdb",
                  "fagsystem": {
                    "kode": "K9SAK",
                    "kodeverk": "FAGSYSTEM"
                  },
                  "saksnummer": "5YC4K",
                  "aktørId": "9906098522415",
                  "behandlingId": 1000001,
                  "eventTid": "2020-02-20T07:38:49",
                  "eventHendelse": "BEHANDLINGSKONTROLL_EVENT",
                  "behandlinStatus": "UTRED",
                   "behandlingstidFrist": "2020-03-31",
                  "behandlingStatus": "UTRED",
                  "behandlingSteg": "INREG_AVSL",
                  "behandlendeEnhet": "0300",
                  "ytelseTypeKode": "OMP",
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

    @KtorExperimentalAPI
    @Test
    fun `Skal ta vare på eksisterende reservasjoner`() {
        val pg = EmbeddedPostgres.start()
        val dataSource = pg.postgresDatabase
        runMigration(dataSource)
        val gosysOppgaveGateway = mockk<GosysOppgaveGateway>()
        val sakOgBehadlingProducer = mockk<SakOgBehadlingProducer>()
        every { gosysOppgaveGateway.hentOppgaver(any()) } returns mutableListOf(GosysOppgave(1, 2))
        every { gosysOppgaveGateway.opprettOppgave(any()) } returns GosysOppgave(1, 3)
        every { sakOgBehadlingProducer.opprettetBehandlng(any(), any()) } just runs
        
        val oppgaveRepository = OppgaveRepository(dataSource = dataSource)
        val config = mockk<Configuration>()
        val k9sakEventHandler = K9sakEventHandler(
            oppgaveRepository,
            BehandlingProsessEventRepository(dataSource = dataSource),
            config = config,
            sakOgBehadlingProducer = sakOgBehadlingProducer
//            gosysOppgaveGateway = gosysOppgaveGateway
        )

        @Language("JSON") val json =
            """{
                  "eksternId": "6b521f78-ef71-43c3-a615-6c2b8bb4dcdb",
                  "fagsystem": {
                    "kode": "K9SAK",
                    "kodeverk": "FAGSYSTEM"
                  },
                  "saksnummer": "5YC4K",
                  "aktørId": "9906098522415",
                  "behandlingId": 1000001,
                  "eventTid": "2020-02-20T07:38:49",
                  "eventHendelse": "BEHANDLINGSKONTROLL_EVENT",
                  "behandlinStatus": "UTRED",
                  "behandlingStatus": "UTRED",
                  "behandlingSteg": "INREG_AVSL",
                   "behandlingstidFrist": "2020-03-31",
                  "behandlendeEnhet": "0300",
                  "ytelseTypeKode": "PSB",
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

        var oppgave =
            oppgaveRepository.hent(UUID.fromString("6b521f78-ef71-43c3-a615-6c2b8bb4dcdb")).sisteOppgave()
        assertTrue { oppgave.aktiv }
        assertTrue(oppgave.aksjonspunkter.lengde() == 1)
        assertTrue(oppgave.reservasjon == null)

        oppgaveRepository.lagre(
            oppgave.eksternId
        ) {
            oppgave.reservasjon = Reservasjon(LocalDateTime.now(), "", "", LocalDateTime.now(), "")
            oppgave
        }

        oppgave =
            oppgaveRepository.hent(UUID.fromString("6b521f78-ef71-43c3-a615-6c2b8bb4dcdb")).sisteOppgave()
        assertTrue(oppgave.reservasjon != null)

        k9sakEventHandler.prosesser(
            objectMapper.readValue(
                """{
                  "eksternId": "6b521f78-ef71-43c3-a615-6c2b8bb4dcdb",
                  "fagsystem": {
                    "kode": "K9SAK",
                    "kodeverk": "FAGSYSTEM"
                  },
                  "saksnummer": "5YC4K",
                  "aktørId": "9906098522415",
                  "behandlingId": 1000001,
                  "eventTid": "2020-02-20T07:38:49",
                  "eventHendelse": "BEHANDLINGSKONTROLL_EVENT",
                  "behandlinStatus": "UTRED",
                   "behandlingstidFrist": "2020-03-31",
                  "behandlingStatus": "UTRED",
                  "behandlingSteg": "INREG_AVSL",
                  "behandlendeEnhet": "0300",
                  "ytelseTypeKode": "OMP",
                  "behandlingTypeKode": "BT-002",
                  "opprettetBehandling": "2020-02-20T07:38:49",
                  "aksjonspunktKoderMedStatusListe": {
                    "5009": "OPPR",
                    "5084": "OPPR"
                  }
                }""", BehandlingProsessEventDto::class.java
            )
        )

        oppgave =
            oppgaveRepository.hent(UUID.fromString("6b521f78-ef71-43c3-a615-6c2b8bb4dcdb")).sisteOppgave()
        assertTrue { oppgave.aktiv }
        assertTrue(oppgave.aksjonspunkter.lengde() == 2)
        assertTrue(oppgave.reservasjon != null)
    }

}
