package no.nav.k9.tjenester.saksbehandler

import io.ktor.application.call
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.withContext
import no.nav.k9.Configuration
import no.nav.k9.integrasjon.rest.CorrelationId
import no.nav.k9.integrasjon.rest.RequestContextService
import no.nav.k9.tjenester.avdelingsleder.InnloggetNavAnsattDto
import org.slf4j.LoggerFactory
import java.util.*

@KtorExperimentalAPI
@KtorExperimentalLocationsAPI
internal fun Route.NavAnsattApis(requestContextService: RequestContextService, configuration: Configuration) {
    @Location("/saksbehandler")
    class getInnloggetBruker

    val log = LoggerFactory.getLogger("Route.NavAnsattApis")
    val gruppenavnSaksbehandler = "0000-GA-k9sak-saksbehandler"
    val gruppenavnVeileder = "0000-GA-k9sak-veileder"
    val gruppenavnBeslutter = "0000-GA-k9sak-beslutter"
    val gruppenavnEgenAnsatt = "0000-GA-GOSYS_UTVIDET"
    val gruppenavnKode6 = "0000-GA-GOSYS_KODE6"
    val gruppenavnKode7 = "0000-GA-GOSYS_KODE7"
    val gruppenavnOppgavestyrer = "0000-GA-k9sak-Oppgavestyrer"



    get { _: getInnloggetBruker ->
        if (configuration.erIkkeLokalt()) {
            val idtoken = call.idToken()
            withContext(
                requestContextService.getCoroutineContext(
                    context = coroutineContext,
                    correlationId = CorrelationId(UUID.randomUUID().toString()),//call.correlationId(),
                    idToken = idtoken
                )
            ) {
                val token = IdToken(idtoken.value)
                call.respond(
                    InnloggetNavAnsattDto(
                        token.getUsername(),
                        token.getName(),
                        kanSaksbehandle = token.erOppgavebehandler(),
                        kanVeilede = true,
                        kanBeslutte = true,
                        kanBehandleKodeEgenAnsatt = token.kanBehandleEgneAnsatte(),
                        kanBehandleKode6 = token.kanBehandleKode6(),
                        kanBehandleKode7 = token.kanBehandleKode7(),
                        kanOppgavestyre = true
                    )
                )
            }
        } else {
            call.respond(
                InnloggetNavAnsattDto(
                    "alexaban",
                    "Saksbehandler Sara",
                    kanSaksbehandle = true,
                    kanVeilede = true,
                    kanBeslutte = true,
                    kanBehandleKodeEgenAnsatt = true,
                    kanBehandleKode6 = true,
                    kanBehandleKode7 = true,
                    kanOppgavestyre = true
                )
            )
        }
    }
}