package no.nav.k9

import io.ktor.application.*
import io.ktor.util.*
import kotlinx.coroutines.channels.Channel
import no.nav.helse.dusseldorf.ktor.health.HealthService
import no.nav.k9.KoinProfile.*
import no.nav.k9.aksjonspunktbehandling.K9TilbakeEventHandler
import no.nav.k9.aksjonspunktbehandling.K9punsjEventHandler
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
import no.nav.k9.integrasjon.k9.IK9SakService
import no.nav.k9.integrasjon.k9.K9SakService
import no.nav.k9.integrasjon.k9.K9SakServiceLocal
import no.nav.k9.integrasjon.kafka.AsynkronProsesseringV1Service
import no.nav.k9.integrasjon.pdl.IPdlService
import no.nav.k9.integrasjon.pdl.PdlService
import no.nav.k9.integrasjon.pdl.PdlServiceLocal
import no.nav.k9.integrasjon.pdl.PdlServicePreprod
import no.nav.k9.integrasjon.rest.IRequestContextService
import no.nav.k9.integrasjon.rest.RequestContextService
import no.nav.k9.integrasjon.rest.RequestContextServiceLocal
import no.nav.k9.integrasjon.sakogbehandling.SakOgBehandlingProducer
import no.nav.k9.tjenester.avdelingsleder.AvdelingslederTjeneste
import no.nav.k9.tjenester.avdelingsleder.nokkeltall.NokkeltallTjeneste
import no.nav.k9.tjenester.driftsmeldinger.DriftsmeldingTjeneste
import no.nav.k9.tjenester.kodeverk.HentKodeverkTjeneste
import no.nav.k9.tjenester.saksbehandler.oppgave.OppgaveTjeneste
import no.nav.k9.tjenester.saksbehandler.saksliste.SakslisteTjeneste
import no.nav.k9.tjenester.sse.SseEvent
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.util.*
import javax.sql.DataSource

@KtorExperimentalAPI
fun selectModuleBasedOnProfile(application: Application, config: Configuration): List<Module> {
    val envModule = when (config.koinProfile()) {
        LOCAL -> localDevConfig(application, config)
        PREPROD -> preprodConfig(application, config)
        PROD -> prodConfig(application, config)
    }
    return listOf(common(application, config), envModule)
}

@KtorExperimentalAPI
fun common(app: Application, config: Configuration) = module {
    single { config.koinProfile() }
    single { config }
    single { app.hikariConfig(config) as DataSource }
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
        Channel<SseEvent>(Channel.UNLIMITED)
    }
    single(named("oppgaveChannel")) {
        Channel<Oppgave>(Channel.UNLIMITED)
    }
    single(named("oppgaveRefreshChannel")) {
        Channel<UUID>(Channel.UNLIMITED)
    }
    single(named("statistikkRefreshChannel")) {
        Channel<Boolean>(Channel.CONFLATED)
    }

    single { OppgaveRepository(get(), get(), get(named("oppgaveRefreshChannel"))) }
    single {
        OppgaveKøRepository(
            dataSource = get(),
            oppgaveKøOppdatert = get(named("oppgaveKøOppdatert")),
            refreshKlienter = get(named("refreshKlienter")),
            oppgaveRefreshChannel = get(named("oppgaveRefreshChannel")),
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
        BehandlingProsessEventK9Repository(get())
    }

    single {
        PunsjEventK9Repository(get())
    }

    single {
        BehandlingProsessEventTilbakeRepository(get())
    }

    single {
        StatistikkRepository(get())
    }

    single {
        SakOgBehandlingProducer(
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
            behandlingProsessEventK9Repository = get(),
            config = config,
            sakOgBehandlingProducer = get(),
            oppgaveKøRepository = get(),
            reservasjonRepository = get(),
            statistikkProducer = get(),
            statistikkChannel = get(named("statistikkRefreshChannel")),
            statistikkRepository = get(),
            saksbehhandlerRepository = get()
        )
    }

    single {
        K9TilbakeEventHandler(
            oppgaveRepository = get(),
            behandlingProsessEventTilbakeRepository = get(),
            config = config,
            sakOgBehandlingProducer = get(),
            oppgaveKøRepository = get(),
            reservasjonRepository = get(),
            statistikkProducer = get(),
            statistikkChannel = get(named("statistikkRefreshChannel")),
            statistikkRepository = get(),
            saksbehhandlerRepository = get()
        )
    }

    single {
        K9punsjEventHandler(
            oppgaveRepository = get(),
            punsjEventK9Repository = get(),
            statistikkChannel = get(named("statistikkRefreshChannel")),
            statistikkRepository = get(),
            oppgaveKøRepository = get(),
            reservasjonRepository = get()
        )
    }


    single {
        AsynkronProsesseringV1Service(
            kafkaConfig = config.getKafkaConfig(),
            configuration = config,
            k9sakEventHandler = get(),
            k9TilbakeEventHandler = get(),
            punsjEventHandler = get()
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
            statistikkRepository = get(),
            oppgaverSomSkalInnPåKøer = get(named("oppgaveChannel"))
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
            configuration = config,
            oppgaverSomSkalInnPåKøer = get(named("oppgaveChannel"))
        )
    }

    single {
        DriftsmeldingTjeneste(driftsmeldingRepository = get())
    }
    single {
        SakslisteTjeneste(oppgaveTjeneste = get(), azureGraphService = get())
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
    single { RequestContextServiceLocal() as IRequestContextService }

    single {
        PdlServiceLocal() as IPdlService
    }
    single {
        K9SakServiceLocal() as IK9SakService
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
    single {
        K9SakService(
            configuration = get(),
            accessTokenClient = get<AccessTokenClientResolver>().naisSts()
        ) as IK9SakService
    }

    single { RequestContextService() as IRequestContextService }

    single {
        PdlServicePreprod(
            baseUrl = config.pdlUrl(),
            accessTokenClient = get<AccessTokenClientResolver>().naisSts(),
            configuration = config
        ) as IPdlService
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
    single {
        K9SakService(
            configuration = get(),
            accessTokenClient = get<AccessTokenClientResolver>().naisSts()
        ) as IK9SakService
    }
    single { RequestContextService() as IRequestContextService }

    single {
        PdlService(
            baseUrl = config.pdlUrl(),
            accessTokenClient = get<AccessTokenClientResolver>().naisSts(),
            configuration = config
        ) as IPdlService
    }
}

