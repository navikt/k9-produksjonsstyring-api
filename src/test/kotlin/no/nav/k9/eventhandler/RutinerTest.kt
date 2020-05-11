package no.nav.k9.eventhandler

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import io.ktor.util.KtorExperimentalAPI
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import no.nav.helse.dusseldorf.ktor.jackson.dusseldorfConfigured
import no.nav.k9.Configuration
import no.nav.k9.aksjonspunktbehandling.K9sakEventHandler
import no.nav.k9.db.runMigration
import no.nav.k9.domene.modell.*
import no.nav.k9.domene.repository.BehandlingProsessEventRepository
import no.nav.k9.domene.repository.OppgaveKøRepository
import no.nav.k9.domene.repository.OppgaveRepository
import no.nav.k9.domene.repository.ReservasjonRepository
import no.nav.k9.integrasjon.sakogbehandling.SakOgBehadlingProducer
import no.nav.k9.kafka.dto.BehandlingProsessEventDto
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

        val reservasjonRepository = ReservasjonRepository(dataSource = dataSource)
        val oppgaveRepository = OppgaveRepository(dataSource = dataSource)
        val oppgaveKøRepository = OppgaveKøRepository(
            dataSource = dataSource,
            oppgaveKøOppdatert = oppgaveKøOppdatert
        )

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
                fomDato = LocalDate.now().minusDays(100),
                tomDato = LocalDate.now().plusDays(100),
                saksbehandlere = mutableListOf()
            )
        }
        val launch = launch {
            oppdatereKø(
                oppgaveKøRepository = oppgaveKøRepository,
                channel = oppgaveKøOppdatert,
                reservasjonRepository = reservasjonRepository,
                oppgaveRepository = oppgaveRepository
            )
        }
        val sakOgBehadlingProducer = mockk<SakOgBehadlingProducer>()
        every { sakOgBehadlingProducer.opprettetBehandlng(any()) } just runs
        every { sakOgBehadlingProducer.avsluttetBehandling(any()) } just runs
        val config = mockk<Configuration>()
        every { config.erLokalt() } returns true
        val k9sakEventHandler = K9sakEventHandler(
            oppgaveRepository,
            BehandlingProsessEventRepository(dataSource = dataSource),
            config = config,
            sakOgBehadlingProducer = sakOgBehadlingProducer,
            oppgaveKøRepository = oppgaveKøRepository,
            reservasjonRepository = reservasjonRepository
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
                "5003": "OPPR"
              }
            }"""
        val objectMapper = jacksonObjectMapper()
            .dusseldorfConfigured().setPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CAMEL_CASE)

        val event = objectMapper.readValue(json, BehandlingProsessEventDto::class.java)
        k9sakEventHandler.prosesser(event)
        launch.cancelAndJoin()
        val hent = oppgaveKøRepository.hent()
        assert(hent[0].oppgaver.toList()[0] == UUID.fromString("6b521f78-ef71-43c3-a615-6c2b8bb4dcdb"))
    }

}
