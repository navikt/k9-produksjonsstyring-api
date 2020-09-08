package no.nav.k9.domene.repository

import io.ktor.util.*
import no.nav.k9.buildAndTestConfig
import no.nav.k9.domene.lager.oppgave.Oppgave
import no.nav.k9.domene.modell.Aksjonspunkter
import no.nav.k9.domene.modell.BehandlingStatus
import no.nav.k9.domene.modell.BehandlingType
import no.nav.k9.domene.modell.FagsakYtelseType
import no.nav.k9.tjenester.avdelingsleder.nokkeltall.AlleOppgaverNyeOgFerdigstilte
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class StatistikkRepositoryTest : KoinTest {
    @get:Rule
    val koinTestRule = KoinTestRule.create {
        modules(buildAndTestConfig())
    }

    @KtorExperimentalAPI
    @Test
    fun skalFylleMedTommeElementerDersomViIkkeHarDataPåDenDagen() {
        
        val statistikkRepository  = get<StatistikkRepository>()
        
        val hentFerdigstilte = statistikkRepository.hentFerdigstilteOgNyeHistorikkMedYtelsetype(1)

        val omsorgspenger = hentFerdigstilte.filter { it.fagsakYtelseType == FagsakYtelseType.OMSORGSPENGER }
        assert(omsorgspenger.size == 5)
        stopKoin()
    }

    @KtorExperimentalAPI
    @Test
    fun skalFylleMedTommeElementerDersomVdiIkkeHarDataPåDenDagenIdempotent() {
     
        val statistikkRepository  = get<StatistikkRepository>()

        val oppgave = Oppgave(
            behandlingId = 78567,
            fagsakSaksnummer = "5Yagdt",
            aktorId = "675864",
            behandlendeEnhet = "Enhet",
            behandlingsfrist = LocalDateTime.now(),
            behandlingOpprettet = LocalDateTime.now().minusDays(23),
            forsteStonadsdag = LocalDate.now().plusDays(6),
            behandlingStatus = BehandlingStatus.OPPRETTET,
            behandlingType = BehandlingType.FORSTEGANGSSOKNAD,
            fagsakYtelseType = FagsakYtelseType.OMSORGSPENGER,
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
        statistikkRepository.lagre(AlleOppgaverNyeOgFerdigstilte(oppgave.fagsakYtelseType, oppgave.behandlingType, oppgave.eventTid.toLocalDate().minusDays(1))){
            it.nye.add(oppgave.eksternId.toString())
            it
        }
        statistikkRepository.lagre(AlleOppgaverNyeOgFerdigstilte(oppgave.fagsakYtelseType, oppgave.behandlingType, oppgave.eventTid.toLocalDate().minusDays(1))){
            it.nye.add(oppgave.eksternId.toString())
            it
        }
        val hentFerdigstilte = statistikkRepository.hentFerdigstilteOgNyeHistorikkMedYtelsetype(1)
        val omsorgspenger = hentFerdigstilte.filter { it.fagsakYtelseType == FagsakYtelseType.OMSORGSPENGER }
        assert(omsorgspenger.size == 5)
        assert(omsorgspenger.find { it.behandlingType == BehandlingType.FORSTEGANGSSOKNAD }?.nye?.size == 1)

    }
}

