package no.nav.k9.tjenester.saksbehandler.oppgave

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import io.ktor.util.*
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import no.nav.k9.Configuration
import no.nav.k9.KoinProfile
import no.nav.k9.buildAndTestConfig
import no.nav.k9.db.runMigration
import no.nav.k9.domene.lager.oppgave.Oppgave
import no.nav.k9.domene.modell.*
import no.nav.k9.domene.repository.*
import no.nav.k9.integrasjon.abac.IPepClient
import no.nav.k9.integrasjon.abac.PepClientLocal
import no.nav.k9.integrasjon.azuregraph.AzureGraphService
import no.nav.k9.integrasjon.pdl.PdlService
import no.nav.k9.integrasjon.pdl.PersonPdl
import no.nav.k9.integrasjon.pdl.PersonPdlResponse
import no.nav.k9.tjenester.sse.SseEvent
import org.junit.Rule
import org.junit.Test
import org.koin.core.qualifier.named
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class OppgaveTjenesteSettSkjermetTest : KoinTest {
    @KtorExperimentalAPI
    @get:Rule
    val koinTestRule = KoinTestRule.create {
        modules(buildAndTestConfig(mockk()))
    }    
   
    @KtorExperimentalAPI
    @Test
    fun testSettSkjermet() = runBlocking{
       
        val pepClient = get<IPepClient>()

        coEvery { pepClient.harTilgangTilKode6() } returns false
        coEvery { pepClient.erSakKode6(any()) } returns false
        coEvery { pepClient.erSakKode7EllerEgenAnsatt(any()) } returns false
        val oppgaveKøOppdatert = Channel<UUID>(1)
        val refreshKlienter = Channel<SseEvent>(1000)

        val oppgaveRepository = get<OppgaveRepository>()
        val oppgaveKøRepository = OppgaveKøRepository(
            dataSource = get(),
            oppgaveKøOppdatert = oppgaveKøOppdatert,
            refreshKlienter = refreshKlienter,
            pepClient = pepClient
        )
        val saksbehandlerRepository = SaksbehandlerRepository(dataSource = get(),
            pepClient = pepClient)
        val reservasjonRepository = ReservasjonRepository(
            oppgaveKøRepository = oppgaveKøRepository,
            oppgaveRepository = oppgaveRepository,
            dataSource = get(),
            refreshKlienter = refreshKlienter,
            saksbehandlerRepository = saksbehandlerRepository
        )
        val config = mockk<Configuration>()
        val pdlService = mockk<PdlService>()
        val statistikkRepository = StatistikkRepository(dataSource = get())
    
        val azureGraphService = mockk<AzureGraphService>()
        val oppgaveTjeneste = OppgaveTjeneste(
            oppgaveRepository,
            oppgaveKøRepository,
            saksbehandlerRepository,
            pdlService,
            reservasjonRepository, config, azureGraphService, pepClient, statistikkRepository,
            get(named("oppgaveChannel"))
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
            avklarMedlemskap = false, kode6 = false, utenlands = false, vurderopptjeningsvilkåret = false
        )
        oppgaveRepository.lagre(oppgave1.eksternId) { oppgave1 }
        oppgaveko.leggOppgaveTilEllerFjernFraKø(oppgave1, reservasjonRepository)
        oppgaveKøRepository.lagre(oppgaveko.id) {
            oppgaveko
        }
        every { config.koinProfile() } returns KoinProfile.LOCAL
        coEvery { pdlService.person(any()) } returns PersonPdlResponse(false, PersonPdl(
            data = PersonPdl.Data(
                hentPerson = PersonPdl.Data.HentPerson(
                    listOf(
                        element =
                        PersonPdl.Data.HentPerson.Folkeregisteridentifikator("012345678901")
                    ),
                    navn = listOf(
                        PersonPdl.Data.HentPerson.Navn(
                            etternavn = "Etternavn",
                            forkortetNavn = "ForkortetNavn",
                            fornavn = "Fornavn",
                            mellomnavn = null
                        )
                    ),
                    kjoenn = listOf(
                        PersonPdl.Data.HentPerson.Kjoenn(
                            "KVINNE"
                        )
                    ),
                    doedsfall = emptyList()
                )
            )
        ))
        coEvery { pepClient.harBasisTilgang() } returns true
        coEvery { pepClient.harTilgangTilLesSak(any(),any()) } returns true

        var oppgaver = oppgaveTjeneste.hentNesteOppgaverIKø(oppgaveko.id)
        assert(oppgaver.size == 1)
        
        coEvery { pepClient.erSakKode7EllerEgenAnsatt(any()) } returns true

        oppgaveTjeneste.settSkjermet(oppgave1)
    
        oppgaver = oppgaveTjeneste.hentNesteOppgaverIKø(oppgaveko.id)
        assert(oppgaver.isEmpty())

    }

    @KtorExperimentalAPI
    @Test
    fun `hent fagsak`(){
        val pg = EmbeddedPostgres.start()
        val dataSource = pg.postgresDatabase
        runMigration(dataSource)

        val oppgaveKøOppdatert = Channel<UUID>(1)
        val refreshKlienter = Channel<SseEvent>(1000)
        val oppgaverRefresh = Channel<Oppgave>(1000)

        val oppgaveRepository = OppgaveRepository(dataSource = dataSource,pepClient = PepClientLocal(), refreshOppgave = oppgaverRefresh)
        val oppgaveKøRepository = OppgaveKøRepository(
            dataSource = dataSource,
            oppgaveKøOppdatert = oppgaveKøOppdatert,
            refreshKlienter = refreshKlienter,
            pepClient = PepClientLocal()
        )
        val pdlService = mockk<PdlService>()
        val saksbehandlerRepository = SaksbehandlerRepository(dataSource = dataSource,
            pepClient = PepClientLocal())

        val statistikkRepository = StatistikkRepository(dataSource = dataSource)
        val pepClient = mockk<IPepClient>()
        val azureGraphService = mockk<AzureGraphService>()
        val config = mockk<Configuration>()
        val reservasjonRepository = ReservasjonRepository(
            oppgaveKøRepository = oppgaveKøRepository,
            oppgaveRepository = oppgaveRepository,
            dataSource = dataSource,
            refreshKlienter = refreshKlienter,
            saksbehandlerRepository = saksbehandlerRepository
        )
        val oppgaveTjeneste = OppgaveTjeneste(
            oppgaveRepository,
            oppgaveKøRepository,
            saksbehandlerRepository,
            pdlService,
            reservasjonRepository, config, azureGraphService, pepClient, statistikkRepository,
            get(named("oppgaveChannel"))
        )

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

        coEvery {  azureGraphService.hentIdentTilInnloggetBruker() } returns "123"
        every { config.koinProfile() } returns KoinProfile.LOCAL
        coEvery { pepClient.harTilgangTilLesSak(any(), any()) } returns true
        coEvery { pdlService.person(any()) } returns PersonPdlResponse(false, PersonPdl(data = PersonPdl.Data(
            hentPerson = PersonPdl.Data.HentPerson(
                folkeregisteridentifikator = listOf(PersonPdl.Data.HentPerson.Folkeregisteridentifikator("12345678901")),
                navn = listOf(
                    PersonPdl.Data.HentPerson.Navn(
                        etternavn = "etternavn",
                        forkortetNavn = null,
                        fornavn = "fornavn",
                        mellomnavn = null
                    )),
                kjoenn = listOf(PersonPdl.Data.HentPerson.Kjoenn("K")),
                doedsfall = listOf()
            )
        )))

        runBlocking {
            val fagsaker = oppgaveTjeneste.søkFagsaker("Yz647")
            assert(fagsaker.fagsaker.isNotEmpty())
        }
    }

    @Test
    fun hentReservasjonsHistorikk() = runBlocking {
        val pg = EmbeddedPostgres.start()
        val dataSource = pg.postgresDatabase
        runMigration(dataSource)
        val oppgaveKøOppdatert = Channel<UUID>(1)
        val refreshKlienter = Channel<SseEvent>(1000)

        val oppgaverRefresh = Channel<Oppgave>(1000)

        val oppgaveRepository = OppgaveRepository(dataSource = dataSource,pepClient = PepClientLocal(), refreshOppgave = oppgaverRefresh)
        val oppgaveKøRepository = OppgaveKøRepository(
            dataSource = dataSource,
            oppgaveKøOppdatert = oppgaveKøOppdatert,
            refreshKlienter = refreshKlienter,
            pepClient = PepClientLocal()
        )
        val saksbehandlerRepository = SaksbehandlerRepository(dataSource = dataSource,
            pepClient = PepClientLocal())
        val reservasjonRepository = ReservasjonRepository(
            oppgaveKøRepository = oppgaveKøRepository,
            oppgaveRepository = oppgaveRepository,
            dataSource = dataSource,
            refreshKlienter = refreshKlienter,
            saksbehandlerRepository = saksbehandlerRepository
        )
        val config = mockk<Configuration>()
        val pdlService = mockk<PdlService>()
        val statistikkRepository = StatistikkRepository(dataSource = dataSource)
        val pepClient = mockk<IPepClient>()
        val azureGraphService = mockk<AzureGraphService>()

        coEvery {  azureGraphService.hentIdentTilInnloggetBruker() } returns "123"
        val oppgaveTjeneste = OppgaveTjeneste(
            oppgaveRepository,
            oppgaveKøRepository,
            saksbehandlerRepository,
            pdlService,
            reservasjonRepository, config, azureGraphService, pepClient, statistikkRepository,
            get(named("oppgaveChannel"))
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
            avklarMedlemskap = false, kode6 = false, utenlands = false, vurderopptjeningsvilkåret = false
        )
        oppgaveRepository.lagre(oppgave1.eksternId) { oppgave1 }
        oppgaveko.leggOppgaveTilEllerFjernFraKø(oppgave1, reservasjonRepository)
        oppgaveKøRepository.lagre(oppgaveko.id) {
            oppgaveko
        }
        every { config.koinProfile() } returns KoinProfile.LOCAL
        coEvery { pepClient.harBasisTilgang() } returns true
        coEvery { pepClient.harTilgangTilLesSak(any(),any()) } returns true
        coEvery { pepClient.harTilgangTilReservingAvOppgaver() } returns true
        coEvery { pdlService.person(any()) } returns PersonPdlResponse(false, PersonPdl(
            data = PersonPdl.Data(
                hentPerson = PersonPdl.Data.HentPerson(
                    listOf(
                        element =
                        PersonPdl.Data.HentPerson.Folkeregisteridentifikator("012345678901")
                    ),
                    navn = listOf(
                        PersonPdl.Data.HentPerson.Navn(
                            etternavn = "Etternavn",
                            forkortetNavn = "ForkortetNavn",
                            fornavn = "Fornavn",
                            mellomnavn = null
                        )
                    ),
                    kjoenn = listOf(
                        PersonPdl.Data.HentPerson.Kjoenn(
                            "KVINNE"
                        )
                    ),
                    doedsfall = emptyList()
                )
            )
        ))


        var oppgaver = oppgaveTjeneste.hentNesteOppgaverIKø(oppgaveko.id)
        assert(oppgaver.size == 1)
        val oppgave = oppgaver.get(0)

        saksbehandlerRepository.addSaksbehandler(Saksbehandler(brukerIdent = "123", navn= null, epost = "test@test.no", enhet = null))
        saksbehandlerRepository.addSaksbehandler(Saksbehandler(brukerIdent="ny", navn=null,epost =  "test2@test.no",enhet = null))

        oppgaveTjeneste.reserverOppgave("123", oppgave.eksternId)
        oppgaveTjeneste.flyttReservasjon(oppgave.eksternId, "ny", "Ville ikke ha oppgaven")
        val reservasjonsHistorikk = oppgaveTjeneste.hentReservasjonsHistorikk(oppgave.eksternId)

        assert(reservasjonsHistorikk.reservasjoner.size == 2)
        assert(reservasjonsHistorikk.reservasjoner[0].flyttetAv == "123")
    }

}