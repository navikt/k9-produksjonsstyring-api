package no.nav.k9.tjenester.saksbehandler

import io.ktor.application.call
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.withContext
import no.nav.k9.AccessTokenClientResolver
import no.nav.k9.Configuration
import no.nav.k9.integrasjon.pdl.PdlService
import no.nav.k9.integrasjon.rest.CorrelationId
import no.nav.k9.integrasjon.rest.RequestContextService
import no.nav.k9.integrasjon.sakogbehandling.sendTilKø
import org.slf4j.LoggerFactory
import java.util.*

@KtorExperimentalAPI
@KtorExperimentalLocationsAPI
internal fun Route.TestApis(
    requestContextService: RequestContextService,
    pdlService: PdlService,
    accessTokenClientResolver: AccessTokenClientResolver,
    configuration: Configuration
) {

    val log = LoggerFactory.getLogger("Route.TestApis")
    val gruppenavnSaksbehandler = "0000-GA-k9sak-saksbehandler"
    val gruppenavnVeileder = "0000-GA-k9sak-veileder"
    val gruppenavnBeslutter = "0000-GA-k9sak-beslutter"
    val gruppenavnEgenAnsatt = "0000-GA-GOSYS_UTVIDET"
    val gruppenavnKode6 = "0000-GA-GOSYS_KODE6"
    val gruppenavnKode7 = "0000-GA-GOSYS_KODE7"
    val gruppenavnOppgavestyrer = "0000-GA-k9sak-Oppgavestyrer"


    @Location("/testToken")
    class getInnloggetBrukerToken
    get { _: getInnloggetBrukerToken ->
        val idtoken = call.idToken()
        withContext(
            requestContextService.getCoroutineContext(
                context = coroutineContext,
                correlationId = CorrelationId(UUID.randomUUID().toString()),//call.correlationId(),
                idToken = idtoken
            )
        ) {
            call.respond(
                "id: " + idtoken.ident.value + "\n"
                        + "token: " + idtoken.value
                        + "\nnaistoken: " + accessTokenClientResolver.naisSts()
                    .getAccessToken(setOf("openid")).accessToken + "\n"
                        + "azuretoken: " + accessTokenClientResolver.accessTokenClient()
                    .getAccessToken(setOf("api://${accessTokenClientResolver.azureClientId()}/.default")).accessToken
            )
        }
    }
    
    @Location("/test")
    class getInnloggetBruker

    get { _: getInnloggetBruker ->
        withContext(
            requestContextService.getCoroutineContext(
                context = coroutineContext,
                correlationId = CorrelationId(UUID.randomUUID().toString()),//call.correlationId(),
                idToken = call.idToken()
            )
        ) {
            // val client = AbacClient(configuration.abacClient())
            // client.evaluate(AbacRequest(mapOf(Category.AccessSubject to )))
            call.respond(
                sendTilKø(
                    "{\"behandlingId\":1006113,\"fagsakSaksnummer\":\"5YRWM\",\"aktorId\":\"1288870094724\",\"behandlendeEnhet\":\"event.behandlendeEnhet\",\"behandlingsfrist\":\"2020-04-20T21:52:43.372765\",\"behandlingOpprettet\":\"2020-04-19T21:52:30\",\"forsteStonadsdag\":\"2020-04-19\",\"behandlingStatus\":{\"kode\":\"OPPRE\"},\"behandlingType\":{\"kode\":\"BT\n" +
                            "-002\",\"kodeverk\":\"BEHANDLING_TYPE\",\"navn\":\"Førstegangsbehandling\"},\"fagsakYtelseType\":{\"kode\":\"OMP\",\"kodeverk\":\"FAGSAK_YTELSE_TYPE\",\"navn\":\"Omsorgspenger\"},\"aktiv\":true,\"system\":\"K9SAK\",\"oppgaveAvsluttet\":null,\"utfortFraAdmin\":false,\"eksternId\":\"b045eb00-00bc-47c2-8b38-75bbed3b2328\",\"reservasjon\":null,\"oppgaveEgenskap\":[],\"beslutterOppgave\":true,\"aksjonspunkte\n" +
                            "}}", configuration
                )
            )
        }
    }
}