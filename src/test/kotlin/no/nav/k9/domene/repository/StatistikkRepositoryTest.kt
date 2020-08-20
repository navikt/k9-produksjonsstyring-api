package no.nav.k9.domene.repository

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import io.ktor.util.KtorExperimentalAPI
import no.nav.k9.db.runMigration
import no.nav.k9.domene.lager.oppgave.Oppgave
import no.nav.k9.domene.modell.Aksjonspunkter
import no.nav.k9.domene.modell.BehandlingStatus
import no.nav.k9.domene.modell.BehandlingType
import no.nav.k9.domene.modell.FagsakYtelseType
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class StatistikkRepositoryTest {
//    @KtorExperimentalAPI
//    @Test
//    fun skalLagreStatistikk() {
//        val pg = EmbeddedPostgres.start()
//        val dataSource = pg.postgresDatabase
//        runMigration(dataSource)
//
//        val statistikkRepository = StatistikkRepository(dataSource)
//        val eksternId = UUID.randomUUID()
//        val eksternId2 = UUID.randomUUID()
//        val eksternId3 = UUID.randomUUID()
//
//        statistikkRepository.lagreFerdigstilt(BehandlingType.FORSTEGANGSSOKNAD.kode, eksternId)
//        statistikkRepository.lagreFerdigstilt(BehandlingType.FORSTEGANGSSOKNAD.kode, eksternId2)
//        statistikkRepository.lagreFerdigstilt(BehandlingType.FORSTEGANGSSOKNAD.kode, eksternId3)
//
//        val hentFerdigstilte = statistikkRepository.hentFerdigstilte()
//        assertEquals(3, hentFerdigstilte[0].antall)
//    }
//    @KtorExperimentalAPI
//    @Test
//    fun skalHenteferdigstilteHistorikk() {
//        val pg = EmbeddedPostgres.start()
//        val dataSource = pg.postgresDatabase
//        runMigration(dataSource)
//
//        val statistikkRepository = StatistikkRepository(dataSource)
//        val eksternId = UUID.randomUUID()
//        val eksternId2 = UUID.randomUUID()
//        val eksternId3 = UUID.randomUUID()
//
//        statistikkRepository.lagreFerdigstiltHistorikk(BehandlingType.FORSTEGANGSSOKNAD.kode, FagsakYtelseType.OMSORGSPENGER.kode,eksternId)
//        statistikkRepository.lagreFerdigstiltHistorikk(BehandlingType.FORSTEGANGSSOKNAD.kode, FagsakYtelseType.OMSORGSPENGER.kode,eksternId2)
//        statistikkRepository.lagreFerdigstiltHistorikk(BehandlingType.FORSTEGANGSSOKNAD.kode, FagsakYtelseType.OMSORGSPENGER.kode,eksternId3)
//
//        val hentFerdigstilte = statistikkRepository.hentFerdigstilteOgNyeHistorikkMedYtelsetype(5000)
//        assert(hentFerdigstilte.isNotEmpty())
//    }

    @KtorExperimentalAPI
    @Test   
    fun skalFylleMedTommeElementerDersomViIkkeHarDataPåDenDagen() {
        val pg = EmbeddedPostgres.start()
        val dataSource = pg.postgresDatabase
        runMigration(dataSource)

        val statistikkRepository = StatistikkRepository(dataSource)
        
        val hentFerdigstilte = statistikkRepository.hentFerdigstilteOgNyeHistorikkMedYtelsetype(0)
        val omsorgspenger = hentFerdigstilte.filter { it.fagsakYtelseType == FagsakYtelseType.OMSORGSPENGER }
        assert(omsorgspenger.size == 5)
    }
    
    @KtorExperimentalAPI
    @Test   
    fun skalFylleMedTommeElementerDersomVdiIkkeHarDataPåDenDagenIdempotent() {
        val pg = EmbeddedPostgres.start()
        val dataSource = pg.postgresDatabase
        runMigration(dataSource)

        val statistikkRepository = StatistikkRepository(dataSource)
        
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
            avklarMedlemskap = false, skjermet = false, utenlands = false, vurderopptjeningsvilkåret = false
        )
        statistikkRepository.lagreNyHistorikk(oppgave)
        statistikkRepository.lagreNyHistorikk(oppgave)
        val hentFerdigstilte = statistikkRepository.hentFerdigstilteOgNyeHistorikkMedYtelsetype(0)
        val omsorgspenger = hentFerdigstilte.filter { it.fagsakYtelseType == FagsakYtelseType.OMSORGSPENGER }
        assert(omsorgspenger.size == 5)
        assert(omsorgspenger.find { it.behandlingType == BehandlingType.FORSTEGANGSSOKNAD }?.nye?.size == 1)
    }
}

