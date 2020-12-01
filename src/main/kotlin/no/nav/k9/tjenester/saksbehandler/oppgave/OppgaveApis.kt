package no.nav.k9.tjenester.saksbehandler.oppgave

import io.ktor.application.*
import io.ktor.locations.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import kotlinx.coroutines.withContext
import no.nav.k9.KoinProfile
import no.nav.k9.domene.repository.SaksbehandlerRepository
import no.nav.k9.integrasjon.rest.IRequestContextService
import no.nav.k9.integrasjon.rest.idToken
import no.nav.k9.tjenester.saksbehandler.IdTokenLocal
import no.nav.k9.tjenester.saksbehandler.idToken
import org.koin.ktor.ext.inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

private val log: Logger = LoggerFactory.getLogger("nav.OppgaveApis")

@KtorExperimentalAPI
@KtorExperimentalLocationsAPI
internal fun Route.OppgaveApis() {
    val requestContextService by inject<IRequestContextService>()
    val oppgaveTjeneste by inject<OppgaveTjeneste>()
    val saksbehandlerRepository by inject<SaksbehandlerRepository>()
    val profile by inject<KoinProfile>()

    @Location("/")
    class hentOppgaver

    get { _: hentOppgaver ->
        val queryParameter = call.request.queryParameters["id"]
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
            call.respond(
                oppgaveTjeneste.hentNesteOppgaverIKø(UUID.fromString(queryParameter))
            )
        }
    }

    @Location("/behandlede")
    class getBehandledeOppgaver

    get { _: getBehandledeOppgaver ->
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
            call.respond(oppgaveTjeneste.hentSisteBehandledeOppgaver())
        }

    }

    @Location("/reserverte")
    class getReserverteOppgaver

    get { _: getReserverteOppgaver ->
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
            call.respond(oppgaveTjeneste.hentSisteReserverteOppgaver())
        }

    }

    @Location("/antall")
    class hentAntallOppgaverForOppgavekø

    get { _: hentAntallOppgaverForOppgavekø ->
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
            var uuid = call.request.queryParameters["id"]
            if (uuid.isNullOrBlank()) {
                uuid = UUID.randomUUID().toString()
            }
            call.respond(oppgaveTjeneste.hentAntallOppgaver(UUID.fromString(uuid)!!, true, true))
        }
    }

    @Location("/reserver")
    class reserverOppgave

    post { _: reserverOppgave ->
        val oppgaveId = call.receive<OppgaveId>()
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
            call.respond(
                oppgaveTjeneste.reserverOppgave(
                    saksbehandlerRepository.finnSaksbehandlerMedEpost(kotlin.coroutines.coroutineContext.idToken().getUsername())!!.brukerIdent!!,
                    UUID.fromString(oppgaveId.oppgaveId)
                )
            )
        }
    }

    @Location("/opphev")
    class opphevReservasjon
    post { _: opphevReservasjon ->
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
            val params = call.receive<OpphevReservasjonId>()
            call.respond(oppgaveTjeneste.frigiReservasjon(UUID.fromString(params.oppgaveId), params.begrunnelse))
        }
    }

    @Location("/legg-til-behandlet-sak")
    class leggTilBehandletSak

    post { _: leggTilBehandletSak ->
        val params = call.receive<BehandletOppgave>()

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
            call.respond(
                oppgaveTjeneste.leggTilBehandletOppgave(
                    kotlin.coroutines.coroutineContext.idToken().getUsername(),
                    params
                )
            )
        }

    }

    @Location("/forleng")
    class forlengReservasjon
    post { _: forlengReservasjon ->
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
            val oppgaveId = call.receive<OppgaveId>()
            call.respond(oppgaveTjeneste.forlengReservasjonPåOppgave(UUID.fromString(oppgaveId.oppgaveId)))
        }
    }

    @Location("/flytt")
    class flyttReservasjon

    post { _: flyttReservasjon ->
        val params = call.receive<FlyttReservasjonId>()
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
            call.respond(
                oppgaveTjeneste.flyttReservasjon(
                    UUID.fromString(params.oppgaveId),
                    params.brukerIdent,
                    params.begrunnelse
                )
            )
        }

    }

    @Location("/reservasjon/endre")
    class endreReservasjon

    post { _: endreReservasjon ->
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
            val params = call.receive<ReservasjonEndringDto>()
            call.respond(
                oppgaveTjeneste.endreReservasjonPåOppgave(params)
            )
        }
    }

    @Location("/flytt-til-forrige-saksbehandler")
    class flyttReservasjonTilForrigeSaksbehandler

    post { _: flyttReservasjonTilForrigeSaksbehandler ->
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
            val params = call.receive<OppgaveId>()
            call.respond(
                oppgaveTjeneste.flyttReservasjonTilForrigeSakbehandler(UUID.fromString(params.oppgaveId))
            )
        }
    }

    @Location("/hent-historiske-reservasjoner-på-oppgave")
    class hentHistoriskeReservasjonerPåOppgave

    post { _: hentHistoriskeReservasjonerPåOppgave ->
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
            val params = call.receive<OppgaveId>()
            call.respond(
                oppgaveTjeneste.hentReservasjonsHistorikk(UUID.fromString(params.oppgaveId))
            )
        }
    }

    @Location("/flytt/sok")
    class søkSaksbehandler

    post { _: søkSaksbehandler ->
        val params = call.receive<BrukerIdentDto>()
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
            val sokSaksbehandlerMedIdent = oppgaveTjeneste.sokSaksbehandler(params.brukerIdent)
            if (sokSaksbehandlerMedIdent == null) {
                call.respond("")
            } else {
                call.respond(sokSaksbehandlerMedIdent)
            }
        }
    }

    @Location("/oppgaver-for-fagsaker")
    class oppgaverForFagsaker

    get { _: oppgaverForFagsaker ->
        val saker = call.request.queryParameters["saksnummerListe"]
        val saksnummerliste = saker?.split(",") ?: emptyList()

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
            val oppgaver = oppgaveTjeneste.hentOppgaverFraListe(saksnummerliste)
            val result = mutableListOf<OppgaveDto>()
            if (oppgaver.isNotEmpty()) {
                val oppgaverBySaksnummer = oppgaver.groupBy { it.saksnummer }
                for (entry in oppgaverBySaksnummer.entries) {
                    val x = entry.value.firstOrNull { oppgaveDto -> oppgaveDto.erTilSaksbehandling }
                    if (x != null) {
                        result.add(x)
                    } else {
                        result.add(entry.value.first())
                    }
                }
                call.respond(result)
            } else {
                call.respond(oppgaver)
            }
        }
    }
}
