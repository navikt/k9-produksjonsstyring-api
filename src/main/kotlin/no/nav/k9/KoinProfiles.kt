package no.nav.k9

import io.ktor.application.Application
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.channels.Channel
import no.nav.helse.dusseldorf.ktor.health.HealthService
import no.nav.k9.KoinProfile.*
import no.nav.k9.aksjonspunktbehandling.K9sakEventHandler
import no.nav.k9.db.hikariConfig
import no.nav.k9.domene.lager.oppgave.Oppgave
import no.nav.k9.domene.repository.*
import no.nav.k9.integrasjon.abac.IPepClient
import no.nav.k9.integrasjon.abac.PepClient
import no.nav.k9.integrasjon.abac.PepClientLocal
import no.nav.k9.integrasjon.audit.Auditlogger
import no.nav.k9.integrasjon.azuregraph.AzureGraphService
import no.nav.k9.integrasjon.azuregraph.AzureGraphServiceLocal
import no.nav.k9.integrasjon.azuregraph.IAzureGraphService
import no.nav.k9.integrasjon.datavarehus.StatistikkProducer
import no.nav.k9.integrasjon.kafka.AsynkronProsesseringV1Service
import no.nav.k9.integrasjon.pdl.PdlService
import no.nav.k9.integrasjon.rest.RequestContextService
import no.nav.k9.integrasjon.sakogbehandling.SakOgBehadlingProducer
import no.nav.k9.tjenester.avdelingsleder.AvdelingslederTjeneste
import no.nav.k9.tjenester.avdelingsleder.nokkeltall.NokkeltallTjeneste
import no.nav.k9.tjenester.driftsmeldinger.DriftsmeldingTjeneste
import no.nav.k9.tjenester.kodeverk.HentKodeverkTjeneste
import no.nav.k9.tjenester.saksbehandler.oppgave.OppgaveTjeneste
import no.nav.k9.tjenester.sse.SseEvent
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.util.*
import javax.sql.DataSource

@KtorExperimentalAPI
fun selectModuleBasedOnProfile(application: Application, config: Configuration): List<Module> {
    val envModule = when (config.koinProfile()) {
        TEST -> buildAndTestConfig()
        LOCAL -> localDevConfig(application, config)
        PREPROD -> preprodConfig(application, config)
        PROD -> prodConfig(application, config)
    }
    return listOf(common(application, config), envModule)
}

fun buildAndTestConfig() = module {

}

@KtorExperimentalAPI
fun common(app: Application, config: Configuration) = module {
    single { config.koinProfile() }
    single { config }
    single { app.hikariConfig(config) as DataSource }
    single { OppgaveRepository(get()) }
    single { RequestContextService() }
    single {
        NokkeltallTjeneste(
            oppgaveRepository = get(),
            statistikkRepository = get()
        )
    }
    single(named("oppgaveKøOppdatert")) {
        Channel<UUID>(Channel.UNLIMITED)
    }
    single(named("refreshKlienter")) {
        Channel<SseEvent>()
    }
    single(named("oppgaveChannel")) {
        Channel<Oppgave>(Channel.UNLIMITED)
    }

    single {
        OppgaveKøRepository(
            dataSource = get(),
            oppgaveKøOppdatert = get(named("oppgaveKøOppdatert")),
            refreshKlienter = get(named("refreshKlienter"))
        )
    }

    single {
        SaksbehandlerRepository(
            dataSource = get()
        )
    }

    single {
        DriftsmeldingRepository(
            dataSource = get()
        )
    }

    single {
        ReservasjonRepository(
            oppgaveRepository = get(),
            oppgaveKøRepository = get(),
            dataSource = get(),
            refreshKlienter = get(named("refreshKlienter")),
            saksbehandlerRepository = get()
        )
    }

    single {
        BehandlingProsessEventRepository(get())
    }

    single {
        StatistikkRepository(get())
    }

    single {
        SakOgBehadlingProducer(
            kafkaConfig = config.getKafkaConfig(),
            config = config
        )
    }

    single {
        AccessTokenClientResolver(
            clients = config.clients()
        )
    }



    single {
        PdlService(
            baseUrl = config.pdlUrl(),
            accessTokenClient = get<AccessTokenClientResolver>().naisSts(),
            configuration = config
        )
    }

    single {
        StatistikkProducer(
            kafkaConfig = config.getKafkaConfig(),
            config = config,
            pepClient = get(),
            saksbehandlerRepository = get(),
            reservasjonRepository = get()
        )
    }

    single {
        K9sakEventHandler(
            oppgaveRepository = get(),
            behandlingProsessEventRepository = get(),
            config = config,
            sakOgBehadlingProducer = get(),
            oppgaveKøRepository = get(),
            reservasjonRepository = get(),
            statistikkProducer = get(),
            oppgaverSomSkalInnPåKøer = get(named("oppgaveChannel")),
            statistikkRepository = get()
        )
    }


    single {
        AsynkronProsesseringV1Service(
            kafkaConfig = config.getKafkaConfig(),
            configuration = config,
            k9sakEventHandler = get()
        )
    }

    single {
        OppgaveTjeneste(
            oppgaveRepository = get(),
            oppgaveKøRepository = get(),
            saksbehandlerRepository = get(),
            reservasjonRepository = get(),
            pdlService = get(),
            configuration = config,
            pepClient = get(),
            azureGraphService = get(),
            statistikkRepository = get()
        )
    }

    single {
        AvdelingslederTjeneste(
            oppgaveKøRepository = get(),
            saksbehandlerRepository = get(),
            oppgaveTjeneste = get(),
            reservasjonRepository = get(),
            oppgaveRepository = get(),
            pepClient = get(),
            configuration = config
        )
    }

    single {
        DriftsmeldingTjeneste(driftsmeldingRepository = get())
    }
    single {
        HentKodeverkTjeneste()
    }
    single {
        HealthService(
            healthChecks = get<AsynkronProsesseringV1Service>().isReadyChecks()
        )
    }

}

@KtorExperimentalAPI
fun localDevConfig(app: Application, config: Configuration) = module {
    single {
        AzureGraphServiceLocal(
        ) as IAzureGraphService
    }
    single {
        PepClientLocal() as IPepClient
    }
}

@KtorExperimentalAPI
fun preprodConfig(app: Application, config: Configuration) = module {
    single {
        AzureGraphService(
            accessTokenClient = get<AccessTokenClientResolver>().accessTokenClient()
        ) as IAzureGraphService
    }
    single {
        PepClient(azureGraphService = get(), auditlogger = Auditlogger(config), config = config) as IPepClient
    }
}

@KtorExperimentalAPI
fun prodConfig(app: Application, config: Configuration) = module {
    single {
        AzureGraphService(
            accessTokenClient = get<AccessTokenClientResolver>().accessTokenClient()
        ) as IAzureGraphService
    }
    single {
        PepClient(azureGraphService = get(), auditlogger = Auditlogger(config), config = config) as IPepClient
    } 
}

