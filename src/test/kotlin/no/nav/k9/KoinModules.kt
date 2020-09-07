package no.nav.k9

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import no.nav.k9.db.runMigration
import no.nav.k9.domene.repository.DriftsmeldingRepository
import no.nav.k9.domene.repository.OppgaveRepository
import no.nav.k9.domene.repository.StatistikkRepository
import no.nav.k9.integrasjon.abac.PepClientLocal
import org.koin.dsl.module

fun buildAndTestConfig() = module {
    val pg = EmbeddedPostgres.start()
    val dataSource = pg.postgresDatabase
    runMigration(dataSource)
    single { dataSource }
    single { OppgaveRepository(dataSource = dataSource, pepClient = PepClientLocal()) }
    single { DriftsmeldingRepository(dataSource) }
    single { StatistikkRepository(dataSource) }
}