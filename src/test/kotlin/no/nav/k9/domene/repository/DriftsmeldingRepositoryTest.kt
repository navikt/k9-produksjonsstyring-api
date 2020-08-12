package no.nav.k9.domene.repository

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import io.ktor.util.KtorExperimentalAPI
import no.nav.k9.db.runMigration
import no.nav.k9.tjenester.driftsmeldinger.Driftsmelding
import org.junit.Test
import kotlin.test.assertEquals

class DriftsmeldingRepositoryTest{
    @KtorExperimentalAPI
    @Test
    fun skalLagreDriftsmeldingOgHenteDenIgjen() {
        val pg = EmbeddedPostgres.start()
        val dataSource = pg.postgresDatabase
        runMigration(dataSource)

        val driftsmeldingRepository = DriftsmeldingRepository(dataSource)


        val driftsmelding =
            Driftsmelding(melding = "Driftsmelding")
        driftsmeldingRepository.lagreDriftsmelding(driftsmelding)

        val alle = driftsmeldingRepository.hentAlle()
        assertEquals(driftsmelding.id, alle[0].id)
        assertEquals(driftsmelding.melding, alle[0].melding)
        assertEquals(driftsmelding.aktiv, alle[0].aktiv)

        driftsmeldingRepository.slett(driftsmelding.id)
        val ingen = driftsmeldingRepository.hentAlle()
        assertEquals(0, ingen.size)
    }
    
}