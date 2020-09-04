package no.nav.k9.aksjonspunktbehandling

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import io.ktor.util.*
import io.mockk.*
import kotlinx.coroutines.channels.Channel
import no.nav.helse.dusseldorf.ktor.jackson.dusseldorfConfigured
import no.nav.k9.Configuration
import no.nav.k9.KoinProfile
import no.nav.k9.db.runMigration
import no.nav.k9.domene.lager.oppgave.Oppgave
import no.nav.k9.domene.repository.*
import no.nav.k9.integrasjon.abac.PepClientLocal
import no.nav.k9.integrasjon.datavarehus.StatistikkProducer
import no.nav.k9.integrasjon.gosys.GosysOppgave
import no.nav.k9.integrasjon.gosys.GosysOppgaveGateway
import no.nav.k9.integrasjon.kafka.dto.BehandlingProsessEventDto
import no.nav.k9.integrasjon.sakogbehandling.SakOgBehadlingProducer
import no.nav.k9.tjenester.sse.SseEvent
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

        val oppgaveKøOppdatert = Channel<UUID>(1)
        val oppgaverSomSkalInnPåKøer = Channel<Oppgave>(100)
        val refreshKlienter = Channel<SseEvent>(1)
        val oppgaveRepository = OppgaveRepository(dataSource = dataSource, pepClient = PepClientLocal())
        val saksbehandlerRepository = SaksbehandlerRepository(
            dataSource = dataSource,
            pepClient = PepClientLocal()
        )
        val oppgaveKøRepository = OppgaveKøRepository(
            dataSource = dataSource,
            oppgaveKøOppdatert = oppgaveKøOppdatert,
            refreshKlienter = refreshKlienter,
            pepClient = PepClientLocal()
        )
        val reservasjonRepository = ReservasjonRepository(
            oppgaveKøRepository = oppgaveKøRepository,
            oppgaveRepository = oppgaveRepository,
            dataSource = dataSource,
            refreshKlienter = refreshKlienter,
            saksbehandlerRepository = saksbehandlerRepository
        )
        val gosysOppgaveGateway = mockk<GosysOppgaveGateway>()
        val sakOgBehadlingProducer = mockk<SakOgBehadlingProducer>()
        val statistikkProducer = mockk<StatistikkProducer>()
        every { gosysOppgaveGateway.hentOppgaver(any()) } returns mutableListOf(GosysOppgave(1, 1))
        every { gosysOppgaveGateway.avsluttOppgave(any()) } just Runs
        every { sakOgBehadlingProducer.behandlingOpprettet(any()) } just runs
        every { sakOgBehadlingProducer.avsluttetBehandling(any()) } just runs
        every { statistikkProducer.send(any()) } just runs
        val config = mockk<Configuration>()
        every { KoinProfile.LOCAL == config.koinProfile() } returns true
        val statistikkRepository = StatistikkRepository(dataSource = dataSource)
        val k9sakEventHandler = K9sakEventHandler(
            oppgaveRepository,
            BehandlingProsessEventRepository(dataSource = dataSource),
            config = config,
            sakOgBehadlingProducer = sakOgBehadlingProducer,
            oppgaveKøRepository = oppgaveKøRepository,
            reservasjonRepository = reservasjonRepository,
            statistikkProducer = statistikkProducer,
            oppgaverSomSkalInnPåKøer = oppgaverSomSkalInnPåKøer,
            statistikkRepository = statistikkRepository, saksbehhandlerRepository = saksbehandlerRepository
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
        val oppgave = oppgaveModell
        assertFalse { oppgave.aktiv }
    }

    @KtorExperimentalAPI
    @Test
    fun `Skal lukke oppgave dersom den er satt på vent`() {
        val pg = EmbeddedPostgres.start()
        val dataSource = pg.postgresDatabase
        runMigration(dataSource)
        val oppgaveKøOppdatert = Channel<UUID>(1)
        val oppgaverSomSkalInnPåKøer = Channel<Oppgave>(100)
        val refreshKlienter = Channel<SseEvent>(1)
        val oppgaveRepository = OppgaveRepository(dataSource = dataSource, pepClient = PepClientLocal())
        val saksbehandlerRepository = SaksbehandlerRepository(
            dataSource = dataSource,
            pepClient = PepClientLocal()
        )
        val statistikkRepository = StatistikkRepository(dataSource = dataSource)
        val oppgaveKøRepository = OppgaveKøRepository(
            dataSource = dataSource, oppgaveKøOppdatert = oppgaveKøOppdatert,
            refreshKlienter = refreshKlienter,
            pepClient = PepClientLocal()
        )
        val reservasjonRepository = ReservasjonRepository(
            oppgaveKøRepository = oppgaveKøRepository,
            oppgaveRepository = oppgaveRepository,
            dataSource = dataSource,
            refreshKlienter = refreshKlienter,
            saksbehandlerRepository = saksbehandlerRepository
        )
        val gosysOppgaveGateway = mockk<GosysOppgaveGateway>()
        val sakOgBehadlingProducer = mockk<SakOgBehadlingProducer>()
        val statistikkProducer = mockk<StatistikkProducer>()
        every { gosysOppgaveGateway.hentOppgaver(any()) } returns mutableListOf(GosysOppgave(1, 1))
        every { gosysOppgaveGateway.avsluttOppgave(any()) } just Runs
        every { sakOgBehadlingProducer.behandlingOpprettet(any()) } just runs
        every { sakOgBehadlingProducer.avsluttetBehandling(any()) } just runs
        every { statistikkProducer.send(any()) } just runs
        val config = mockk<Configuration>()
        every { KoinProfile.LOCAL == config.koinProfile() } returns true
        val k9sakEventHandler = K9sakEventHandler(
            OppgaveRepository(dataSource = dataSource, pepClient = PepClientLocal()),
            BehandlingProsessEventRepository(dataSource = dataSource),
            config = config,
            sakOgBehadlingProducer = sakOgBehadlingProducer,
            oppgaveKøRepository = oppgaveKøRepository,
            reservasjonRepository = reservasjonRepository,
            statistikkProducer = statistikkProducer,
            oppgaverSomSkalInnPåKøer = oppgaverSomSkalInnPåKøer,
            statistikkRepository = statistikkRepository,
            saksbehhandlerRepository = saksbehandlerRepository
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
        val oppgaveKøOppdatert = Channel<UUID>(1)
        val oppgaverSomSkalInnPåKøer = Channel<Oppgave>(100)
        val oppgaveRepository = OppgaveRepository(dataSource = dataSource, pepClient = PepClientLocal())
        val refreshKlienter = Channel<SseEvent>(1)
        val statistikkRepository = StatistikkRepository(dataSource = dataSource)
        val saksbehandlerRepository = SaksbehandlerRepository(
            dataSource = dataSource,
            pepClient = PepClientLocal()
        )
        val oppgaveKøRepository = OppgaveKøRepository(
            dataSource = dataSource, oppgaveKøOppdatert = oppgaveKøOppdatert,
            refreshKlienter = refreshKlienter,
            pepClient = PepClientLocal()
        )
        val reservasjonRepository = ReservasjonRepository(
            oppgaveKøRepository = oppgaveKøRepository,
            oppgaveRepository = oppgaveRepository,
            dataSource = dataSource,
            refreshKlienter = refreshKlienter,
            saksbehandlerRepository = saksbehandlerRepository
        )
        val gosysOppgaveGateway = mockk<GosysOppgaveGateway>()
        val sakOgBehadlingProducer = mockk<SakOgBehadlingProducer>()
        val statistikkProducer = mockk<StatistikkProducer>()
        val config = mockk<Configuration>()

        every { gosysOppgaveGateway.hentOppgaver(any()) } returns mutableListOf(GosysOppgave(1, 1))
        every { gosysOppgaveGateway.avsluttOppgave(any()) } just Runs
        every { sakOgBehadlingProducer.behandlingOpprettet(any()) } just runs
        every { statistikkProducer.send(any()) } just runs
        every { KoinProfile.LOCAL == config.koinProfile() } returns true

        val k9sakEventHandler = K9sakEventHandler(
            oppgaveRepository,
            BehandlingProsessEventRepository(dataSource = dataSource),
            config = config,
            sakOgBehadlingProducer = sakOgBehadlingProducer,
            oppgaveKøRepository = oppgaveKøRepository,
            reservasjonRepository = reservasjonRepository,
            statistikkProducer = statistikkProducer,
            oppgaverSomSkalInnPåKøer = oppgaverSomSkalInnPåKøer,
            statistikkRepository = statistikkRepository,
            saksbehhandlerRepository = saksbehandlerRepository
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
            oppgaveRepository.hent(UUID.fromString("6b521f78-ef71-43c3-a615-6c2b8bb4dcdb"))
        assertTrue { oppgave.aktiv }
    }

    @KtorExperimentalAPI
    @Test
    fun `Skal ha 1 oppgave med 3 aksjonspunkter`() {
        val pg = EmbeddedPostgres.start()
        val dataSource = pg.postgresDatabase
        runMigration(dataSource)
        val oppgaveKøOppdatert = Channel<UUID>(1)
        val oppgaverSomSkalInnPåKøer = Channel<Oppgave>(100)
        val refreshKlienter = Channel<SseEvent>(1)
        val statistikkRepository = StatistikkRepository(dataSource = dataSource)
        val oppgaveRepository = OppgaveRepository(dataSource = dataSource, pepClient = PepClientLocal())
        val saksbehandlerRepository = SaksbehandlerRepository(
            dataSource = dataSource,
            pepClient = PepClientLocal()
        )
        val oppgaveKøRepository = OppgaveKøRepository(
            dataSource = dataSource, oppgaveKøOppdatert = oppgaveKøOppdatert,
            refreshKlienter = refreshKlienter,
            pepClient = PepClientLocal()
        )
        val reservasjonRepository = ReservasjonRepository(
            oppgaveKøRepository = oppgaveKøRepository,
            oppgaveRepository = oppgaveRepository,
            dataSource = dataSource,
            refreshKlienter = refreshKlienter,
            saksbehandlerRepository = saksbehandlerRepository
        )
        val gosysOppgaveGateway = mockk<GosysOppgaveGateway>()
        val sakOgBehadlingProducer = mockk<SakOgBehadlingProducer>()
        val statistikkProducer = mockk<StatistikkProducer>()
        val config = mockk<Configuration>()

        every { gosysOppgaveGateway.hentOppgaver(any()) } returns mutableListOf(GosysOppgave(1, 2))
        every { gosysOppgaveGateway.opprettOppgave(any()) } returns GosysOppgave(1, 3)
        every { sakOgBehadlingProducer.behandlingOpprettet(any()) } just runs
        every { statistikkProducer.send(any()) } just runs
        every { KoinProfile.LOCAL == config.koinProfile() } returns true

        val k9sakEventHandler = K9sakEventHandler(
            oppgaveRepository,
            BehandlingProsessEventRepository(dataSource = dataSource),
            config = config,
            sakOgBehadlingProducer = sakOgBehadlingProducer,
            oppgaveKøRepository = oppgaveKøRepository,
            reservasjonRepository = reservasjonRepository,
            statistikkProducer = statistikkProducer,
            oppgaverSomSkalInnPåKøer = oppgaverSomSkalInnPåKøer,
            statistikkRepository = statistikkRepository,
            saksbehhandlerRepository = saksbehandlerRepository
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
            oppgaveRepository.hent(UUID.fromString("6b521f78-ef71-43c3-a615-6c2b8bb4dcdb"))
        assertTrue { oppgave.aktiv }
        assertTrue(oppgave.aksjonspunkter.lengde() == 3)
    }

    @KtorExperimentalAPI
    @Test
    fun `Støtte tilbakekreving`() {
        val pg = EmbeddedPostgres.start()
        val dataSource = pg.postgresDatabase
        runMigration(dataSource)
        val oppgaveKøOppdatert = Channel<UUID>(1)
        val oppgaverSomSkalInnPåKøer = Channel<Oppgave>(100)
        val refreshKlienter = Channel<SseEvent>(1)
        val statistikkRepository = StatistikkRepository(dataSource = dataSource)
        val oppgaveRepository = OppgaveRepository(dataSource = dataSource, pepClient = PepClientLocal())
        val saksbehandlerRepository = SaksbehandlerRepository(
            dataSource = dataSource,
            pepClient = PepClientLocal()
        )
        val oppgaveKøRepository = OppgaveKøRepository(
            dataSource = dataSource, oppgaveKøOppdatert = oppgaveKøOppdatert,
            refreshKlienter = refreshKlienter,
            pepClient = PepClientLocal()
        )
        val reservasjonRepository = ReservasjonRepository(
            oppgaveKøRepository = oppgaveKøRepository,
            oppgaveRepository = oppgaveRepository,
            dataSource = dataSource,
            refreshKlienter = refreshKlienter,
            saksbehandlerRepository = saksbehandlerRepository
        )
        val gosysOppgaveGateway = mockk<GosysOppgaveGateway>()
        val sakOgBehadlingProducer = mockk<SakOgBehadlingProducer>()
        val statistikkProducer = mockk<StatistikkProducer>()
        val config = mockk<Configuration>()

        every { gosysOppgaveGateway.hentOppgaver(any()) } returns mutableListOf(GosysOppgave(1, 2))
        every { gosysOppgaveGateway.opprettOppgave(any()) } returns GosysOppgave(1, 3)
        every { sakOgBehadlingProducer.behandlingOpprettet(any()) } just runs
        every { statistikkProducer.send(any()) } just runs
        every { KoinProfile.LOCAL == config.koinProfile() } returns true

        val k9sakEventHandler = K9sakEventHandler(
            oppgaveRepository,
            BehandlingProsessEventRepository(dataSource = dataSource),
            config = config,
            sakOgBehadlingProducer = sakOgBehadlingProducer,
            oppgaveKøRepository = oppgaveKøRepository,
            reservasjonRepository = reservasjonRepository,
            statistikkProducer = statistikkProducer,
            oppgaverSomSkalInnPåKøer = oppgaverSomSkalInnPåKøer,
            statistikkRepository = statistikkRepository,
            saksbehhandlerRepository = saksbehandlerRepository
        )

        @Language("JSON") val json =
            """{
               "eksternId": "5c7be441-ebf3-4878-9ebc-399635b0a179",
               "fagsystem": "K9TILBAKE",
               "saksnummer": "61613602",
               "aktørId": "9914079721604",
               "behandlingId": null,
               "eventTid": "2020-06-17T13:07:15.674343500",
               "eventHendelse": "AKSJONSPUNKT_OPPRETTET",
               "behandlinStatus": null,
               "behandlingStatus": "UTRED",
               "behandlingSteg": "FAKTFEILUTSTEG",
               "behandlendeEnhet": "4833",
               "ytelseTypeKode": "OMP",
               "behandlingTypeKode": "BT-007",
               "opprettetBehandling": "2020-06-16T13:16:51.690",
               "aksjonspunktKoderMedStatusListe": {
                               "5030": "UTFO",
                               "7002": "UTFO",
                               "7001": "OPPR",
                               "7003": "OPPR"
               },
               "href": "/fpsak/fagsak/61613602/behandling/53/?punkt=default&fakta=default",
               "førsteFeilutbetaling": "2019-10-19",
               "feilutbetaltBeløp": 26820,
               "ansvarligSaksbehandlerIdent": "saksbeh"
}       """

        val event = AksjonspunktLagetTilbake().deserialize(null, json.toByteArray())!!

        k9sakEventHandler.prosesser(event)
        val oppgave =
            oppgaveRepository.hent(UUID.fromString("5c7be441-ebf3-4878-9ebc-399635b0a179"))
        assertTrue { !oppgave.aktiv }
    }

    @KtorExperimentalAPI
    @Test
    fun `Støtte tilbakekreving aksjonspunkt`() {
        val pg = EmbeddedPostgres.start()
        val dataSource = pg.postgresDatabase
        runMigration(dataSource)
        val oppgaveKøOppdatert = Channel<UUID>(1)
        val oppgaverSomSkalInnPåKøer = Channel<Oppgave>(100)
        val refreshKlienter = Channel<SseEvent>(1)
        val statistikkRepository = StatistikkRepository(dataSource = dataSource)
        val oppgaveRepository = OppgaveRepository(dataSource = dataSource, pepClient = PepClientLocal())
        val saksbehandlerRepository = SaksbehandlerRepository(
            dataSource = dataSource,
            pepClient = PepClientLocal()
        )
        val oppgaveKøRepository = OppgaveKøRepository(
            dataSource = dataSource, oppgaveKøOppdatert = oppgaveKøOppdatert,
            refreshKlienter = refreshKlienter,
            pepClient = PepClientLocal()
        )
        val reservasjonRepository = ReservasjonRepository(
            oppgaveKøRepository = oppgaveKøRepository,
            oppgaveRepository = oppgaveRepository,
            dataSource = dataSource,
            refreshKlienter = refreshKlienter,
            saksbehandlerRepository = saksbehandlerRepository
        )
        val gosysOppgaveGateway = mockk<GosysOppgaveGateway>()
        val sakOgBehadlingProducer = mockk<SakOgBehadlingProducer>()
        val statistikkProducer = mockk<StatistikkProducer>()
        val config = mockk<Configuration>()

        every { gosysOppgaveGateway.hentOppgaver(any()) } returns mutableListOf(GosysOppgave(1, 2))
        every { gosysOppgaveGateway.opprettOppgave(any()) } returns GosysOppgave(1, 3)
        every { sakOgBehadlingProducer.behandlingOpprettet(any()) } just runs
        every { statistikkProducer.send(any()) } just runs
        every { KoinProfile.LOCAL == config.koinProfile() } returns true

        val k9sakEventHandler = K9sakEventHandler(
            oppgaveRepository,
            BehandlingProsessEventRepository(dataSource = dataSource),
            config = config,
            sakOgBehadlingProducer = sakOgBehadlingProducer,
            oppgaveKøRepository = oppgaveKøRepository,
            reservasjonRepository = reservasjonRepository,
            statistikkProducer = statistikkProducer,
            oppgaverSomSkalInnPåKøer = oppgaverSomSkalInnPåKøer,
            statistikkRepository = statistikkRepository,
            saksbehhandlerRepository = saksbehandlerRepository
        )

        @Language("JSON") val json =
            """{
               "eksternId": "5c7be441-ebf3-4878-9ebc-399635b0a179",
               "fagsystem": "K9TILBAKE",
               "saksnummer": "61613602",
               "aktørId": "9914079721604",
               "behandlingId": null,
               "eventTid": "2020-06-17T13:13:24.536645200",
               "eventHendelse": "AKSJONSPUNKT_TILBAKEFØR",
               "behandlinStatus": null,
               "behandlingStatus": "UTRED",
               "behandlingSteg": "FAKTFEILUTSTEG",
               "behandlendeEnhet": "1900",
               "ytelseTypeKode": "OMP",
               "behandlingTypeKode": "BT-007",
               "opprettetBehandling": "2020-06-16T13:16:51.690",
               "aksjonspunktKoderMedStatusListe": {
                               "5030": "UTFO",
                               "7002": "UTFO",
                               "7001": "UTFO",
                               "5002": "AVBR",
                               "7003": "UTFO"
               },
               "href": "/fpsak/fagsak/61613602/behandling/53/?punkt=default&fakta=default",
               "førsteFeilutbetaling": "2019-10-19",
               "feilutbetaltBeløp": 26820,
               "ansvarligSaksbehandlerIdent": "saksbeh"
}       """


        val event = AksjonspunktLagetTilbake().deserialize(null, json.toByteArray())!!

        k9sakEventHandler.prosesser(event)
        val oppgave =
            oppgaveRepository.hent(UUID.fromString("5c7be441-ebf3-4878-9ebc-399635b0a179"))
        assertTrue { !oppgave.aktiv }
    }
}
