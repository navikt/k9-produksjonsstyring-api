package no.nav.k9.tjenester.saksbehandler.oppgave

import io.ktor.application.call
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.locations.post
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.withContext
import no.nav.k9.Configuration
import no.nav.k9.integrasjon.rest.RequestContextService
import no.nav.k9.tjenester.saksbehandler.idToken
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

private val logger: Logger = LoggerFactory.getLogger("nav.OppgaveApis")

@KtorExperimentalAPI
@KtorExperimentalLocationsAPI
internal fun Route.OppgaveApis(
    configuration: Configuration,
    requestContextService: RequestContextService,
    oppgaveTjeneste: OppgaveTjeneste
) {

    @Location("/")
    class hentOppgaver

    get { _: hentOppgaver ->
        val queryParameter = call.request.queryParameters["id"]
        if (configuration.erIkkeLokalt) {
            withContext(
                requestContextService.getCoroutineContext(
                    context = coroutineContext,
                    idToken = call.idToken()
                )
            ) {
                call.respond(
                    oppgaveTjeneste.hentNesteOppgaverIKø(call.idToken(), UUID.fromString(queryParameter))
                )
            }
        } else {
            call.respond(
                oppgaveTjeneste.hentNesteOppgaverIKø(kø = UUID.fromString(queryParameter))
            )
        }

    }

    @Location("/resultat")
    class getOppgaverTilBehandling

    get { _: getOppgaverTilBehandling ->
//        val sakslisteId: SakslisteIdDto =
//            ObjectMapper().readValue(call.request.queryParameters["sakslisteId"], SakslisteIdDto::class.java)
//        val nesteOppgaver = oppgaveTjeneste.hentNesteOppgaver(sakslisteId.verdi)

    }

    @Location("/behandlede")
    class getBehandledeOppgaver

    get { _: getBehandledeOppgaver ->

        if (configuration.erIkkeLokalt) {
            val idToken = call.idToken()
            withContext(
                requestContextService.getCoroutineContext(
                    context = coroutineContext,
                    idToken = idToken
                )
            ) {
                call.respond(oppgaveTjeneste.hentSisteReserverteOppgaver(idToken.getUsername()))
            }
        } else {
            call.respond(oppgaveTjeneste.hentSisteReserverteOppgaver("alexaban"))
        }
    }

    @Location("/reserverte")
    class getReserverteOppgaver

    get { _: getReserverteOppgaver ->
        if (configuration.erIkkeLokalt) {
            val idToken = call.idToken()
            withContext(
                requestContextService.getCoroutineContext(
                    context = coroutineContext,
                    idToken = idToken
                )
            ) {
                call.respond(oppgaveTjeneste.hentSisteReserverteOppgaver(idToken.getUsername()))
            }
        } else {
            call.respond(oppgaveTjeneste.hentSisteReserverteOppgaver("alexaban"))
        }
    }

    @Location("/antall")
    class hentAntallOppgaverForOppgavekø

    get { _: hentAntallOppgaverForOppgavekø ->
        var uuid = call.request.queryParameters["id"]
        if (uuid.isNullOrBlank()) {
            uuid = UUID.randomUUID().toString()
        }
        call.respond(oppgaveTjeneste.hentAntallOppgaver(UUID.fromString(uuid)!!))
    }

    @Location("/reserver")
    class reserverOppgave

    post { _: reserverOppgave ->
        val oppgaveId = call.receive<OppgaveId>()

        if (configuration.erIkkeLokalt) {
            val idToken = call.idToken()
            withContext(
                requestContextService.getCoroutineContext(
                    context = coroutineContext,
                    idToken = idToken
                )
            ) {
                call.respond(
                    oppgaveTjeneste.reserverOppgave(
                        idToken.getUsername(),
                        UUID.fromString(oppgaveId.oppgaveId)
                    )
                )
            }
        } else {
            call.respond(oppgaveTjeneste.reserverOppgave("alexaban", UUID.fromString(oppgaveId.oppgaveId)))
        }
    }

    @Location("/opphev")
    class opphevReservasjon

    post { _: opphevReservasjon ->
        val params = call.receive<OpphevReservasjonId>()
        call.respond(oppgaveTjeneste.frigiReservasjon(UUID.fromString(params.oppgaveId), params.begrunnelse))
    }

    @Location("/forleng")
    class forlengReservasjon

    post { _: forlengReservasjon ->
        val oppgaveId = call.receive<OppgaveId>()
        call.respond(oppgaveTjeneste.forlengReservasjonPåOppgave(UUID.fromString(oppgaveId.oppgaveId)))
    }

    @Location("/flytt")
    class flyttReservasjon

    post { _: flyttReservasjon ->
        val params = call.receive<FlyttReservasjonId>()
        call.respond(
            oppgaveTjeneste.flyttReservasjon(
                UUID.fromString(params.oppgaveId),
                params.brukernavn,
                params.begrunnelse
            )
        )
    }

    @Location("/oppgaver-for-fagsaker")
    class oppgaverForFagsaker

    get { _: oppgaverForFagsaker ->
        var saker = call.request.queryParameters["saksnummerListe"]
        val saksnummerliste = saker?.split(",") ?: emptyList()

        if (configuration.erIkkeLokalt) {
            withContext(
                requestContextService.getCoroutineContext(
                    context = coroutineContext,
                    idToken = call.idToken()
                )
            ) { call.respond(oppgaveTjeneste.hentOppgaverFraListe(saksnummerliste)) }
        }
    }
}
