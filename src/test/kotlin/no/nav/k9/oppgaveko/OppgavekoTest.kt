package no.nav.k9.oppgaveko

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import io.ktor.util.KtorExperimentalAPI
import io.mockk.*
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import no.nav.helse.dusseldorf.ktor.jackson.dusseldorfConfigured
import no.nav.k9.Configuration
import no.nav.k9.aksjonspunktbehandling.K9sakEventHandler
import no.nav.k9.db.runMigration
import no.nav.k9.domene.lager.oppgave.Oppgave
import no.nav.k9.domene.modell.*
import no.nav.k9.domene.repository.*
import no.nav.k9.eventhandler.oppdatereKø
import no.nav.k9.integrasjon.abac.PepClient
import no.nav.k9.integrasjon.kafka.dto.BehandlingProsessEventDto
import no.nav.k9.integrasjon.pdl.PdlService
import no.nav.k9.integrasjon.sakogbehandling.SakOgBehadlingProducer
import no.nav.k9.tjenester.avdelingsleder.oppgaveko.AndreKriterierDto
import no.nav.k9.tjenester.saksbehandler.oppgave.OppgaveTjeneste
import org.intellij.lang.annotations.Language
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class OppgavekoTest {
    @KtorExperimentalAPI
    @Test
    fun `Oppgavene tilfredsstiller filtreringskriteriene i køen`() = runBlocking {
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
        val config = mockk<Configuration>()
        val pdlService = mockk<PdlService>()
        val saksbehandlerRepository = SaksbehandlerRepository(dataSource=dataSource)
        val pepClient = mockk<PepClient>()
        val oppgaveTjeneste = OppgaveTjeneste(oppgaveRepository,
                oppgaveKøRepository,
                saksbehandlerRepository,
                pdlService,
                reservasjonRepository, config, pepClient)
        val uuid = UUID.randomUUID()
        val oppgaveko = OppgaveKø(
                id = uuid,
                navn = "Ny kø",
                sistEndret = LocalDate.now(),
                sortering = KøSortering.OPPRETT_BEHANDLING,
                filtreringBehandlingTyper = mutableListOf(BehandlingType.FORSTEGANGSSOKNAD),
                filtreringYtelseTyper = mutableListOf(FagsakYtelseType.PLEIEPENGER_SYKT_BARN),
                filtreringAndreKriterierType = mutableListOf(
                        AndreKriterierDto(
                                uuid.toString(),
                                AndreKriterierType.PAPIRSØKNAD,
                                true,
                                true),
                        AndreKriterierDto(
                                uuid.toString(),
                                AndreKriterierType.SELVSTENDIG_FRILANS,
                                true,
                                false)),
                enhet = Enhet.NASJONAL,
                fomDato = LocalDate.now().minusDays(100),
                tomDato = LocalDate.now().plusDays(100),
                saksbehandlere = mutableListOf()
        )
        oppgaveKøRepository.lagre(uuid) { oppgaveko }

        val oppgave1 = Oppgave(
                9438,
                "Yz647",
                "273857",
                "Enhet",
                LocalDateTime.now(),
                LocalDateTime.now().minusDays(23),
                LocalDate.now().plusDays(6),
                BehandlingStatus.OPPRETTET,
                BehandlingType.FORSTEGANGSSOKNAD,
                FagsakYtelseType.PLEIEPENGER_SYKT_BARN,
                true,
                "system",
                null,
                false,
                UUID.randomUUID(),
                emptyList(),
                Aksjonspunkter(emptyMap()),
                true,
                false,
                false,
                false,
                false,
                true,
                false,
                false)
        val oppgave2 = Oppgave(
                78567,
                "5Yagdt",
                "675864",
                "Enhet",
                LocalDateTime.now(),
                LocalDateTime.now().minusDays(23),
                LocalDate.now().plusDays(6),
                BehandlingStatus.OPPRETTET,
                BehandlingType.FORSTEGANGSSOKNAD,
                FagsakYtelseType.PLEIEPENGER_SYKT_BARN,
                true,
                "system",
                null,
                false,
                UUID.randomUUID(),
                emptyList(),
                Aksjonspunkter(emptyMap()),
                true,
                false,
                true,
                false,
                false,
                true,
                false,
                false)

        oppgaveRepository.lagre(UUID.randomUUID()) { oppgave1 }
        oppgaveRepository.lagre(UUID.randomUUID()) { oppgave2 }

        oppgaveko.leggOppgaveTilEllerFjernFraKø(oppgave1, reservasjonRepository)
        oppgaveko.leggOppgaveTilEllerFjernFraKø(oppgave2, reservasjonRepository)
        oppgaveKøRepository.lagre(oppgaveko.id){oppgaveko}
        every { config.erLokalt() } returns true

        val hent = oppgaveTjeneste.hentOppgaver(uuid)
        assert(hent.size == 1)
        assert(hent[0].registrerPapir)
        assert(!hent[0].selvstendigFrilans)
    }
}

