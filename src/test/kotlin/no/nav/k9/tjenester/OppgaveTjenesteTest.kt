package no.nav.k9.tjenester

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import io.ktor.util.KtorExperimentalAPI
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import no.nav.k9.Configuration
import no.nav.k9.db.runMigration
import no.nav.k9.domene.lager.oppgave.Oppgave
import no.nav.k9.domene.modell.*
import no.nav.k9.domene.repository.OppgaveKøRepository
import no.nav.k9.domene.repository.OppgaveRepository
import no.nav.k9.domene.repository.ReservasjonRepository
import no.nav.k9.domene.repository.SaksbehandlerRepository
import no.nav.k9.integrasjon.abac.PepClient
import no.nav.k9.integrasjon.azuregraph.AzureGraphService
import no.nav.k9.integrasjon.pdl.PdlService
import no.nav.k9.tjenester.saksbehandler.oppgave.OppgaveTjeneste
import no.nav.k9.tjenester.sse.SseEvent
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class OppgaveTjenesteTest {
    @KtorExperimentalAPI
    @Test
    fun `Returnerer korrekte tall for nye og ferdistilte oppgaver`() = runBlocking {
        val pg = EmbeddedPostgres.start()
        val dataSource = pg.postgresDatabase
        runMigration(dataSource)
        val oppgaveKøOppdatert = Channel<UUID>(1)
        val refreshKlienter = Channel<SseEvent>(1000)

        val oppgaveRepository = OppgaveRepository(dataSource = dataSource)
        val oppgaveKøRepository = OppgaveKøRepository(
                dataSource = dataSource,
                oppgaveKøOppdatert = oppgaveKøOppdatert,
                oppgaveRepository = oppgaveRepository,
            refreshKlienter = refreshKlienter
        )
        val reservasjonRepository = ReservasjonRepository(
            oppgaveKøRepository = oppgaveKøRepository,
            oppgaveRepository = oppgaveRepository,
            dataSource = dataSource,
            refreshKlienter = refreshKlienter
        )
        val config = mockk<Configuration>()
        val pdlService = mockk<PdlService>()
        val saksbehandlerRepository = SaksbehandlerRepository(dataSource = dataSource)
        val pepClient = mockk<PepClient>()
        val azureGraphService = mockk<AzureGraphService>()
        val oppgaveTjeneste = OppgaveTjeneste(
                oppgaveRepository,
                oppgaveKøRepository,
                saksbehandlerRepository,
                pdlService,
                reservasjonRepository, config, azureGraphService, pepClient
        )
        val uuid = UUID.randomUUID()
        val oppgaveko = OppgaveKø(
                id = uuid,
                navn = "Ny kø",
                sistEndret = LocalDate.now(),
                sortering = KøSortering.OPPRETT_BEHANDLING,
                filtreringBehandlingTyper = mutableListOf(BehandlingType.FORSTEGANGSSOKNAD, BehandlingType.INNSYN),
                filtreringYtelseTyper = mutableListOf(),
                filtreringAndreKriterierType = mutableListOf(),
                enhet = Enhet.NASJONAL,
                fomDato = null,
                tomDato = null,
                saksbehandlere = mutableListOf()
        )
        oppgaveKøRepository.lagre(uuid) { oppgaveko }

        val oppgave1 = Oppgave(
                behandlingId = 9438,
                fagsakSaksnummer = "Yz647",
                aktorId = "273857",
                behandlendeEnhet = "Enhet",
                behandlingsfrist = LocalDateTime.now(),
                behandlingOpprettet = LocalDateTime.now(),
                forsteStonadsdag = LocalDate.now().plusDays(6),
                behandlingStatus = BehandlingStatus.OPPRETTET,
                behandlingType = BehandlingType.FORSTEGANGSSOKNAD,
                fagsakYtelseType = FagsakYtelseType.PLEIEPENGER_SYKT_BARN,
                aktiv = true,
                system = "system",
                oppgaveAvsluttet = null,
                utfortFraAdmin = false,
                eksternId = UUID.randomUUID(),
                oppgaveEgenskap = emptyList(),
                aksjonspunkter = Aksjonspunkter(emptyMap()),
                tilBeslutter = true,
                utbetalingTilBruker = false,
                selvstendigFrilans = false,
                kombinert = false,
                søktGradering = false,
                registrerPapir = true,
                årskvantum = false,
                avklarMedlemskap = false, skjermet = false, utenlands = false, vurderopptjeningsvilkåret = false
        )
        val oppgave2 = Oppgave(
                behandlingId = 78567,
                fagsakSaksnummer = "5Yagdt",
                aktorId = "675864",
                behandlendeEnhet = "Enhet",
                behandlingsfrist = LocalDateTime.now(),
                behandlingOpprettet = LocalDateTime.now().minusDays(23),
                forsteStonadsdag = LocalDate.now().plusDays(6),
                behandlingStatus = BehandlingStatus.OPPRETTET,
                behandlingType = BehandlingType.FORSTEGANGSSOKNAD,
                fagsakYtelseType = FagsakYtelseType.PLEIEPENGER_SYKT_BARN,
                aktiv = true,
                system = "system",
                oppgaveAvsluttet = null,
                utfortFraAdmin = false,
                eksternId = UUID.randomUUID(),
                oppgaveEgenskap = emptyList(),
                aksjonspunkter = Aksjonspunkter(emptyMap()),
                tilBeslutter = true,
                utbetalingTilBruker = false,
                selvstendigFrilans = true,
                kombinert = false,
                søktGradering = false,
                registrerPapir = true,
                årskvantum = false,
                avklarMedlemskap = false, skjermet = false, utenlands = false, vurderopptjeningsvilkåret = false
        )

        val oppgave3 = Oppgave(
                behandlingId = 78567,
                fagsakSaksnummer = "rty79",
                aktorId = "675864",
                behandlendeEnhet = "Enhet",
                behandlingsfrist = LocalDateTime.now(),
                behandlingOpprettet = LocalDateTime.now().minusDays(23),
                forsteStonadsdag = LocalDate.now().plusDays(6),
                behandlingStatus = BehandlingStatus.OPPRETTET,
                behandlingType = BehandlingType.INNSYN,
                fagsakYtelseType = FagsakYtelseType.PLEIEPENGER_SYKT_BARN,
                aktiv = true,
                system = "system",
                oppgaveAvsluttet = null,
                utfortFraAdmin = false,
                eksternId = UUID.randomUUID(),
                oppgaveEgenskap = emptyList(),
                aksjonspunkter = Aksjonspunkter(emptyMap()),
                tilBeslutter = true,
                utbetalingTilBruker = false,
                selvstendigFrilans = true,
                kombinert = false,
                søktGradering = false,
                registrerPapir = true,
                årskvantum = false,
                avklarMedlemskap = false, skjermet = false, utenlands = false, vurderopptjeningsvilkåret = false
        )

        val oppgave4 = Oppgave(
                behandlingId = 78567,
                fagsakSaksnummer = "klgre89",
                aktorId = "675864",
                behandlendeEnhet = "Enhet",
                behandlingsfrist = LocalDateTime.now(),
                behandlingOpprettet = LocalDateTime.now().minusDays(23),
                forsteStonadsdag = LocalDate.now().plusDays(6),
                behandlingStatus = BehandlingStatus.AVSLUTTET,
                behandlingType = BehandlingType.REVURDERING,
                fagsakYtelseType = FagsakYtelseType.PLEIEPENGER_SYKT_BARN,
                aktiv = true,
                system = "system",
                oppgaveAvsluttet = LocalDateTime.now(),
                utfortFraAdmin = false,
                eksternId = UUID.randomUUID(),
                oppgaveEgenskap = emptyList(),
                aksjonspunkter = Aksjonspunkter(emptyMap()),
                tilBeslutter = true,
                utbetalingTilBruker = false,
                selvstendigFrilans = true,
                kombinert = false,
                søktGradering = false,
                registrerPapir = true,
                årskvantum = false,
                avklarMedlemskap = false, skjermet = false, utenlands = false, vurderopptjeningsvilkåret = false
        )

        oppgaveRepository.lagre(oppgave1.eksternId) { oppgave1 }
        oppgaveRepository.lagre(oppgave2.eksternId) { oppgave2 }
        oppgaveRepository.lagre(oppgave3.eksternId) { oppgave3 }
        oppgaveRepository.lagre(oppgave4.eksternId) { oppgave4 }

        oppgaveko.leggOppgaveTilEllerFjernFraKø(oppgave1, reservasjonRepository)
        oppgaveko.leggOppgaveTilEllerFjernFraKø(oppgave2, reservasjonRepository)
        oppgaveko.leggOppgaveTilEllerFjernFraKø(oppgave3, reservasjonRepository)
        oppgaveko.leggOppgaveTilEllerFjernFraKø(oppgave4, reservasjonRepository)
        oppgaveKøRepository.lagre(oppgaveko.id) {
            oppgaveko
        }
        every { config.erLokalt() } returns true
        val hent = oppgaveTjeneste.hentNyeOgFerdigstilteOppgaver(oppgaveko.id.toString())
        assert(hent.size == 2)
        assert(hent[0].behandlingType == BehandlingType.FORSTEGANGSSOKNAD)
        assert(hent[0].antallFerdigstilte == 0)
        assert(hent[0].antallNye == 2)
        assert(hent[1].behandlingType == BehandlingType.INNSYN)
        assert(hent[1].antallFerdigstilte == 0)
        assert(hent[1].antallNye == 1)
    }
}
