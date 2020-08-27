package no.nav.k9.tjenester.avdelingsleder.oppgaveko

import io.ktor.application.*
import io.ktor.locations.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.coroutines.withContext
import no.nav.k9.KoinProfile
import no.nav.k9.integrasjon.rest.IRequestContextService
import no.nav.k9.tjenester.avdelingsleder.AvdelingslederTjeneste
import no.nav.k9.tjenester.saksbehandler.IdTokenLocal
import no.nav.k9.tjenester.saksbehandler.idToken
import org.koin.ktor.ext.inject
import java.util.*

@KtorExperimentalLocationsAPI
fun Route.AvdelingslederOppgavekøApis() {
    val avdelingslederTjeneste by inject<AvdelingslederTjeneste>()
    val requestContextService by inject<IRequestContextService>()
    val profile by inject<KoinProfile>()

    @Location("/")
    class hentAlleOppgaveKøer

    get { _: hentAlleOppgaveKøer ->
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
            call.respond(avdelingslederTjeneste.hentOppgaveKøer())
        }
    }

    class opprettOppgaveKø

    post { _: opprettOppgaveKø ->
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
            call.respond(avdelingslederTjeneste.opprettOppgaveKø())
        }
    }

    @Location("/navn")
    class endreOppgavekoNavn

    post { _: endreOppgavekoNavn ->
        val uuid = call.receive<OppgavekøNavnDto>()
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
            call.respond(avdelingslederTjeneste.endreOppgavekøNavn(uuid))
        }
    }

    @Location("/slett")
    class slettOppgaveKø

    post { _: slettOppgaveKø ->
        val uuid = call.receive<IdDto>()
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
            call.respond(avdelingslederTjeneste.slettOppgavekø(UUID.fromString(uuid.id)))
        }
    }

    @Location("/behandlingstype")
    class lagreBehandlingstype

    post { _: lagreBehandlingstype ->
        val behandling = call.receive<BehandlingsTypeDto>()
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
            call.respond(avdelingslederTjeneste.endreBehandlingsType(behandling))
        }
    }

    @Location("/skjermet")
    class lagreSkjermet

    post { _: lagreSkjermet ->
        val behandling = call.receive<SkjermetDto>()
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
            call.respond(avdelingslederTjeneste.endreSkjerming(behandling))
        }
    }

    @Location("/ytelsetype")
    class lagreYtelsestype
    post { _: lagreYtelsestype ->
        val ytelse = call.receive<YtelsesTypeDto>()
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
            call.respond(avdelingslederTjeneste.endreYtelsesType(ytelse))
        }
    }

    @Location("/andre-kriterier")
    class endreKriterier

    post { _: endreKriterier ->
        val kriterium = call.receive<AndreKriterierDto>()
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
            call.respond(avdelingslederTjeneste.endreKriterium(kriterium))
        }
    }

    @Location("/sortering")
    class lagreSortering

    post { _: lagreSortering ->
        val sortering = call.receive<KøSorteringDto>()
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
            call.respond(avdelingslederTjeneste.endreKøSortering(sortering))
        }
    }

    @Location("/sortering-tidsintervall-dato")
    class lagreSorteringType

    post { _: lagreSorteringType ->
        val sortering = call.receive<SorteringDatoDto>()
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
            call.respond(avdelingslederTjeneste.endreKøSorteringDato(sortering))
        }
    }

    @Location("/saksbehandler")
    class leggFjernSaksbehandlerOppgaveko

    post { _: leggFjernSaksbehandlerOppgaveko ->
        val saksbehandler = call.receive<SaksbehandlerOppgavekoDto>()
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
            call.respond(avdelingslederTjeneste.leggFjernSaksbehandlerOppgavekø(saksbehandler))
        }
    }
}
