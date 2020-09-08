package no.nav.k9

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import io.ktor.util.*
import no.nav.k9.db.runMigration
import no.nav.k9.domene.repository.DriftsmeldingRepository
import no.nav.k9.domene.repository.OppgaveRepository
import no.nav.k9.domene.repository.StatistikkRepository
import no.nav.k9.integrasjon.abac.IPepClient
import no.nav.k9.integrasjon.abac.PepClientLocal
import org.koin.core.module.Module
import org.koin.dsl.module

@KtorExperimentalAPI
fun buildAndTestConfig(pepClient: IPepClient = PepClientLocal()): Module = module {
    val pg = EmbeddedPostgres.start()
    val dataSource = pg.postgresDatabase
    runMigration(dataSource)
    single { dataSource }
    single { pepClient }
    single { OppgaveRepository(dataSource = dataSource, pepClient = get()) }
    single { DriftsmeldingRepository(dataSource) }
    single { StatistikkRepository(dataSource) }
}