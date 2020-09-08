package no.nav.k9

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import io.ktor.util.*
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.channels.Channel
import no.nav.k9.db.runMigration
import no.nav.k9.domene.lager.oppgave.Oppgave
import no.nav.k9.domene.repository.*
import no.nav.k9.integrasjon.abac.IPepClient
import no.nav.k9.integrasjon.abac.PepClientLocal
import no.nav.k9.integrasjon.azuregraph.AzureGraphServiceLocal
import no.nav.k9.integrasjon.azuregraph.IAzureGraphService
import no.nav.k9.integrasjon.pdl.IPdlService
import no.nav.k9.integrasjon.pdl.PdlServiceLocal
import no.nav.k9.tjenester.saksbehandler.oppgave.OppgaveTjeneste
import no.nav.k9.tjenester.sse.SseEvent
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.util.*

@KtorExperimentalAPI
fun buildAndTestConfig(pepClient: IPepClient = PepClientLocal()): Module = module {
    val pg = EmbeddedPostgres.start()
    val dataSource = pg.postgresDatabase
    runMigration(dataSource)

    single(named("oppgaveKøOppdatert")) {
        Channel<UUID>(Channel.UNLIMITED)
    }
    single(named("refreshKlienter")) {
        Channel<SseEvent>(Channel.UNLIMITED)
    }
    single(named("oppgaveChannel")) {
        Channel<Oppgave>(Channel.UNLIMITED)
    }

    single { dataSource }
    single { pepClient }
    single { OppgaveRepository(dataSource = get(), pepClient = get()) }
    single { DriftsmeldingRepository(get()) }
    single { StatistikkRepository(get()) }

    single {
        OppgaveKøRepository(
            dataSource = get(),
            oppgaveKøOppdatert = get(named("oppgaveKøOppdatert")),
            refreshKlienter = get(named("refreshKlienter")),
            pepClient = get()
        )
    }
    single {
        SaksbehandlerRepository(
            dataSource = get(),
            pepClient = get()
        )
    }

    single {
        ReservasjonRepository(
            oppgaveKøRepository = get(),
            oppgaveRepository = get(),
            dataSource = get(),
            refreshKlienter = get(named("refreshKlienter")),
            saksbehandlerRepository = get()
        )
    }
    val configuration = mockk<Configuration>()
    single {
        configuration
    }
    every { KoinProfile.LOCAL == configuration.koinProfile() } returns true

    single {
        PdlServiceLocal() as IPdlService
    }
    single {
        AzureGraphServiceLocal(
        ) as IAzureGraphService
    }
    single {
        OppgaveTjeneste(
            get(),
            get(),
            get(),
            get(),
            get(), get(), get(), get(), get()
        )
    }
}