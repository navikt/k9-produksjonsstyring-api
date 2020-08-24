package no.nav.k9.eventhandler

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import io.ktor.util.*
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import no.nav.helse.dusseldorf.ktor.jackson.dusseldorfConfigured
import no.nav.k9.Configuration
import no.nav.k9.KoinProfile
import no.nav.k9.aksjonspunktbehandling.K9sakEventHandler
import no.nav.k9.db.runMigration
import no.nav.k9.domene.lager.oppgave.Oppgave
import no.nav.k9.domene.modell.*
import no.nav.k9.domene.repository.*
import no.nav.k9.integrasjon.abac.PepClientLocal
import no.nav.k9.integrasjon.datavarehus.StatistikkProducer
import no.nav.k9.integrasjon.kafka.dto.BehandlingProsessEventDto
import no.nav.k9.integrasjon.sakogbehandling.SakOgBehadlingProducer
import no.nav.k9.tjenester.sse.SseEvent
import org.intellij.lang.annotations.Language
import org.junit.Test
import java.time.LocalDate
import java.util.*

class RutinerTest {
    @KtorExperimentalAPI
    @Test
    fun `Tilordne oppgave til oppgavekø dersom oppgaven tilfredsstiller kriteriene til køen`() = runBlocking {
        val pg = EmbeddedPostgres.start()
        val dataSource = pg.postgresDatabase
        runMigration(dataSource)
        val oppgaveKøOppdatert = Channel<UUID>(1)
        val oppgaverSomSkalInnPåKøer = Channel<Oppgave>(100)
        val refreshKlienter = Channel<SseEvent>(100)
        val statistikkProducer = mockk<StatistikkProducer>()
        val oppgaveRepository = OppgaveRepository(dataSource = dataSource)
        val  saksbehandlerRepository = SaksbehandlerRepository(dataSource = dataSource,
            pepClient = PepClientLocal()
        )
        val oppgaveKøRepository = OppgaveKøRepository(
            dataSource = dataSource,
            oppgaveKøOppdatert = oppgaveKøOppdatert,
            refreshKlienter = refreshKlienter,
            pepClient = PepClientLocal()
        )
        val statistikkRepository = StatistikkRepository(dataSource = dataSource)
        val reservasjonRepository = ReservasjonRepository(
            oppgaveKøRepository = oppgaveKøRepository,
            oppgaveRepository = oppgaveRepository,
            dataSource = dataSource,
            refreshKlienter = refreshKlienter,
            saksbehandlerRepository = saksbehandlerRepository
        )
        every { statistikkProducer.send(any()) } just runs
        val uuid = UUID.randomUUID()
        oppgaveKøRepository.lagre(uuid) {
            OppgaveKø(
                id = uuid,
                navn = "Ny kø",
                sistEndret = LocalDate.now(),
                sortering = KøSortering.OPPRETT_BEHANDLING,
                filtreringBehandlingTyper = mutableListOf(BehandlingType.FORSTEGANGSSOKNAD),
                filtreringYtelseTyper = mutableListOf(FagsakYtelseType.PLEIEPENGER_SYKT_BARN),
                filtreringAndreKriterierType = mutableListOf(),
                enhet = Enhet.NASJONAL,
                fomDato = null,
                tomDato = null,
                saksbehandlere = mutableListOf()
            )
        }
        val launch = GlobalScope.launch {
            oppdatereKø(
                channel = oppgaveKøOppdatert,
                oppgaveKøRepository = oppgaveKøRepository,
                oppgaveRepository = oppgaveRepository,
                reservasjonRepository = reservasjonRepository
            )
        }
        val launch2 = GlobalScope.launch {
            oppdatereKøerMedOppgave(
                channel = oppgaverSomSkalInnPåKøer,
                oppgaveKøRepository = oppgaveKøRepository,
                reservasjonRepository = reservasjonRepository
            )
        }
        val sakOgBehadlingProducer = mockk<SakOgBehadlingProducer>()
        every { sakOgBehadlingProducer.behandlingOpprettet(any()) } just runs
        every { sakOgBehadlingProducer.avsluttetBehandling(any()) } just runs
        val config = mockk<Configuration>()
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
            statistikkRepository = statistikkRepository
        )

        k9sakEventHandler.prosesser(getEvent("5YC4K"))
        k9sakEventHandler.prosesser(getEvent("5YC4K1"))
        k9sakEventHandler.prosesser(getEvent("5YC4K2"))
        k9sakEventHandler.prosesser(getEvent("5YC4K3"))
        k9sakEventHandler.prosesser(getEvent("5YC4K4"))
        launch.cancelAndJoin()
        launch2.cancelAndJoin()
        var hent = oppgaveKøRepository.hent()
        while (hent.isEmpty() || hent[0].oppgaverOgDatoer.toList().isEmpty()) {
            hent = oppgaveKøRepository.hent()
        }
        assert(hent[0].oppgaverOgDatoer[0].id == UUID.fromString("6b521f78-ef71-43c3-a615-6c2b8bb4dcdb"))
    }

    private fun getEvent(id: String): BehandlingProsessEventDto {
        @Language("JSON") val json =
            """{
                  "eksternId": "6b521f78-ef71-43c3-a615-6c2b8bb4dcdb",
                  "fagsystem": {
                    "kode": "K9SAK",
                    "kodeverk": "FAGSYSTEM"
                  },
                  "saksnummer": "${id}",
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
                    "5020": "OPPR"
                  }
                }"""
        val objectMapper = jacksonObjectMapper()
            .dusseldorfConfigured().setPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CAMEL_CASE)

        return objectMapper.readValue(json, BehandlingProsessEventDto::class.java)
    }

}
