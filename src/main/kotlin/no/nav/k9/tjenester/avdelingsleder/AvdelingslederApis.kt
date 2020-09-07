package no.nav.k9.tjenester.avdelingsleder

import io.ktor.application.*
import io.ktor.locations.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import kotlinx.coroutines.withContext
import no.nav.k9.KoinProfile
import no.nav.k9.integrasjon.rest.IRequestContextService
import no.nav.k9.tjenester.saksbehandler.IdTokenLocal
import no.nav.k9.tjenester.saksbehandler.idToken
import no.nav.k9.tjenester.saksbehandler.oppgave.OppgaveId
import no.nav.k9.tjenester.saksbehandler.oppgave.OppgaveTjeneste
import org.koin.ktor.ext.inject
import java.util.*

@KtorExperimentalAPI
@KtorExperimentalLocationsAPI
internal fun Route.AvdelingslederApis() {
    val oppgaveTjeneste by inject<OppgaveTjeneste>()
    val avdelingslederTjeneste by inject<AvdelingslederTjeneste>()
    val requestContextService by inject<IRequestContextService>()
    val profile by inject<KoinProfile>()
    
    @Location("/oppgaver/antall-totalt")
    class hentAntallOppgaverTotalt

    get { _: hentAntallOppgaverTotalt ->
        withContext(
            requestContextService.getCoroutineContext(
                context = coroutineContext,
                idToken = if (profile != KoinProfile.LOCAL) {
                    call.idToken()
                } else {
                    IdTokenLocal()
                }
            )
        ) {
            call.respond(oppgaveTjeneste.hentAntallOppgaverTotalt())
        }

    }

    @Location("/oppgaver/antall")
    class hentAntallOppgaver()

    get { _: hentAntallOppgaver ->
        val uuid = call.parameters["id"]
        withContext(
            requestContextService.getCoroutineContext(
                context = coroutineContext,
                idToken = if (profile != KoinProfile.LOCAL) {
                    call.idToken()
                } else {
                    IdTokenLocal()
                }
            )
        ) {
            call.respond(oppgaveTjeneste.hentAntallOppgaver(UUID.fromString(uuid), true))
        }
    }

    @Location("/saksbehandlere")
    class hentSaksbehandlere

    get { _: hentSaksbehandlere ->
        withContext(
            requestContextService.getCoroutineContext(
                context = coroutineContext,
                idToken = if (profile != KoinProfile.LOCAL) {
                    call.idToken()
                } else {
                    IdTokenLocal()
                }
            )
        ) {
            call.respond(avdelingslederTjeneste.hentSaksbehandlere())
        }
    }

    @Location("/saksbehandlere/sok")
    class søkSaksbehandler

    post { _: søkSaksbehandler ->
        val epost = call.receive<EpostDto>()
        withContext(
            requestContextService.getCoroutineContext(
                context = coroutineContext,
                idToken = if (profile != KoinProfile.LOCAL) {
                    call.idToken()
                } else {
                    IdTokenLocal()
                }
            )
        ) {
            call.respond(avdelingslederTjeneste.søkSaksbehandler(epost))
        }
    }


    @Location("/saksbehandlere/slett")
    class slettSaksbehandler

    post { _: slettSaksbehandler ->
        val epost = call.receive<EpostDto>()
        withContext(
            requestContextService.getCoroutineContext(
                context = coroutineContext,
                idToken = if (profile != KoinProfile.LOCAL) {
                    call.idToken()
                } else {
                    IdTokenLocal()
                }
            )
        ) {
            call.respond(avdelingslederTjeneste.fjernSaksbehandler(epost.epost))
        }
    }

    @Location("/reservasjoner")
    class hentReservasjoner

    get { _: hentReservasjoner ->
            withContext(
                requestContextService.getCoroutineContext(
                    context = coroutineContext,
                    idToken = if (profile != KoinProfile.LOCAL) {
                        call.idToken()
                    } else {
                        IdTokenLocal()
                    }
                )
            ) {
                call.respond(avdelingslederTjeneste.hentAlleReservasjoner())
            }
      
    }

    @Location("/reservasjoner/opphev")
    class opphevReservasjon

    post { _: opphevReservasjon ->
        val params = call.receive<OppgaveId>()
        withContext(
            requestContextService.getCoroutineContext(
                context = coroutineContext,
                idToken = if (profile != KoinProfile.LOCAL) {
                    call.idToken()
                } else {
                    IdTokenLocal()
                }
            )
        ) {
            call.respond(avdelingslederTjeneste.opphevReservasjon(UUID.fromString(params.oppgaveId)))
        }
    }
}
