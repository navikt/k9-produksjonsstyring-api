package no.nav.k9.tjenester.saksbehandler.oppgave

import io.ktor.util.*
import kotlinx.coroutines.runBlocking
import no.nav.k9.buildAndTestConfig
import no.nav.k9.domene.lager.oppgave.Oppgave
import no.nav.k9.domene.modell.*
import no.nav.k9.domene.repository.OppgaveKøRepository
import no.nav.k9.domene.repository.OppgaveRepository
import no.nav.k9.domene.repository.ReservasjonRepository
import no.nav.k9.domene.repository.SaksbehandlerRepository
import org.junit.Rule
import org.junit.Test
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class OppgaveTjenesteTest : KoinTest {
    @KtorExperimentalAPI
    @get:Rule
    val koinTestRule = KoinTestRule.create {
        modules(buildAndTestConfig())
    }

    @KtorExperimentalAPI
    @Test
    fun `Returnerer korrekte tall for nye og ferdistilte oppgaver`() = runBlocking {

        val oppgaveRepository = get<OppgaveRepository>()
        val oppgaveKøRepository = get<OppgaveKøRepository>()
        val reservasjonRepository = get<ReservasjonRepository>()
        val oppgaveTjeneste = get<OppgaveTjeneste>()
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
            avklarMedlemskap = false, kode6 = false, utenlands = false, vurderopptjeningsvilkåret = false
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
            avklarMedlemskap = false, kode6 = false, utenlands = false, vurderopptjeningsvilkåret = false
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
            avklarMedlemskap = false, kode6 = false, utenlands = false, vurderopptjeningsvilkåret = false
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
            avklarMedlemskap = false, kode6 = false, utenlands = false, vurderopptjeningsvilkåret = false
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
            it!!.nyeOgFerdigstilteOppgaver(oppgave1).leggTilNy(oppgave1.eksternId.toString())
            it.nyeOgFerdigstilteOppgaver(oppgave2).leggTilNy(oppgave2.eksternId.toString())
            it.nyeOgFerdigstilteOppgaver(oppgave3).leggTilNy(oppgave3.eksternId.toString())
            it.nyeOgFerdigstilteOppgaver(oppgave4).leggTilNy(oppgave4.eksternId.toString())
            it
        }

        val hent = oppgaveTjeneste.hentNyeOgFerdigstilteOppgaver(oppgaveko.id.toString())
        assert(hent.size == 3)
    }

    @KtorExperimentalAPI
    @Test
    fun `hent fagsak`() {
        val oppgaveRepository = get<OppgaveRepository>()
        val oppgaveTjeneste = get<OppgaveTjeneste>()

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
            avklarMedlemskap = false, kode6 = false, utenlands = false, vurderopptjeningsvilkåret = false
        )
        oppgaveRepository.lagre(oppgave1.eksternId) { oppgave1 }

        runBlocking {
            val fagsaker = oppgaveTjeneste.søkFagsaker("Yz647")
            assert(fagsaker.fagsaker.isNotEmpty())
        }
    }

    @Test
    fun hentReservasjonsHistorikk() = runBlocking {
        val oppgaveRepository = get<OppgaveRepository>()
        val oppgaveTjeneste = get<OppgaveTjeneste>()
        val oppgaveKøRepository = get<OppgaveKøRepository>()
        val reservasjonRepository = get<ReservasjonRepository>()
        val saksbehandlerRepository = get<SaksbehandlerRepository>()
        
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
            avklarMedlemskap = false, kode6 = false, utenlands = false, vurderopptjeningsvilkåret = false
        )
        oppgaveRepository.lagre(oppgave1.eksternId) { oppgave1 }
        oppgaveko.leggOppgaveTilEllerFjernFraKø(oppgave1, reservasjonRepository)
        oppgaveKøRepository.lagre(oppgaveko.id) {
            oppgaveko
        }
        

        var oppgaver = oppgaveTjeneste.hentNesteOppgaverIKø(oppgaveko.id)
        assert(oppgaver.size == 1)
        val oppgave = oppgaver.get(0)

        saksbehandlerRepository.addSaksbehandler(
            Saksbehandler(
                brukerIdent = "123",
                navn = null,
                epost = "test@test.no",
                enhet = null
            )
        )
        saksbehandlerRepository.addSaksbehandler(
            Saksbehandler(
                brukerIdent = "ny",
                navn = null,
                epost = "test2@test.no",
                enhet = null
            )
        )

        oppgaveTjeneste.reserverOppgave("123", oppgave.eksternId)
        oppgaveTjeneste.flyttReservasjon(oppgave.eksternId, "ny", "Ville ikke ha oppgaven")
        val reservasjonsHistorikk = oppgaveTjeneste.hentReservasjonsHistorikk(oppgave.eksternId)

        assert(reservasjonsHistorikk.reservasjoner.size == 2)
        assert(reservasjonsHistorikk.reservasjoner[0].flyttetAv == "saksbehandler@nav.no")
    }

}
