package no.nav.k9.domene.repository

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import io.ktor.util.KtorExperimentalAPI
import no.nav.k9.db.runMigration
import no.nav.k9.domene.modell.BehandlingType
import org.junit.Test
import java.util.*
import kotlin.test.assertEquals

class StatistikkRepositoryTest{
    @KtorExperimentalAPI
    @Test
    fun skalLagreStatistikk() {
        val pg = EmbeddedPostgres.start()
        val dataSource = pg.postgresDatabase
        runMigration(dataSource)

        val statistikkRepository = StatistikkRepository(dataSource)
        val eksternId = UUID.randomUUID()
        val eksternId2 = UUID.randomUUID()
        val eksternId3 = UUID.randomUUID()

        statistikkRepository.lagreFerdigstilt(BehandlingType.FORSTEGANGSSOKNAD.kode, eksternId)
        statistikkRepository.lagreFerdigstilt(BehandlingType.FORSTEGANGSSOKNAD.kode, eksternId2)
        statistikkRepository.lagreFerdigstilt(BehandlingType.FORSTEGANGSSOKNAD.kode, eksternId3)

        val hentFerdigstilte = statistikkRepository.hentFerdigstilte()
        assertEquals(3, hentFerdigstilte[0].antall)
    }
}