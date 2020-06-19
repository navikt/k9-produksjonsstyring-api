package no.nav.k9.db

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import io.ktor.util.KtorExperimentalAPI
import no.nav.k9.domene.lager.oppgave.Oppgave
import no.nav.k9.domene.modell.Aksjonspunkter
import no.nav.k9.domene.modell.BehandlingStatus
import no.nav.k9.domene.modell.BehandlingType
import no.nav.k9.domene.modell.FagsakYtelseType
import no.nav.k9.domene.repository.OppgaveRepository
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class RekkefølgeJsonBArrayTest {
    @KtorExperimentalAPI
    @Test
    fun `EndrerRekkefølgenIArrayeneSeg`() {
        val pg = EmbeddedPostgres.start()
        val dataSource = pg.postgresDatabase
        runMigration(dataSource)

        val oppgaveRepository = OppgaveRepository(dataSource = dataSource)

        val eksternId = UUID.randomUUID()
        IntRange(1, 10).forEach {
            lagreOppgave(eksternId, oppgaveRepository, it)
            assert(oppgaveRepository.hent()[0].behandlendeEnhet == "Enhet$it")
        }

    }

    private fun lagreOppgave(
        eksternId: UUID,
        oppgaveRepository: OppgaveRepository,
        i: Int
    ) {
        val oppgave1 = Oppgave(
            behandlingId = 9438,
            fagsakSaksnummer = "Yz647",
            aktorId = "273857",
            behandlendeEnhet = "Enhet$i",
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
            eksternId = eksternId,
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
        oppgaveRepository.lagre(oppgave1.eksternId) { oppgave1 }
    }
}

