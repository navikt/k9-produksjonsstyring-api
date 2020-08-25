package no.nav.k9.tjenester.saksbehandler.saksliste

import io.ktor.application.*
import io.ktor.locations.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import kotlinx.coroutines.withContext
import no.nav.k9.KoinProfile
import no.nav.k9.domene.modell.OppgaveKø
import no.nav.k9.domene.repository.OppgaveKøRepository
import no.nav.k9.integrasjon.abac.IPepClient
import no.nav.k9.integrasjon.rest.IRequestContextService
import no.nav.k9.integrasjon.rest.idToken
import no.nav.k9.tjenester.saksbehandler.IdTokenLocal
import no.nav.k9.tjenester.saksbehandler.idToken
import no.nav.k9.tjenester.saksbehandler.oppgave.OppgaveTjeneste
import org.koin.ktor.ext.inject
import java.util.*

@KtorExperimentalAPI
@KtorExperimentalLocationsAPI
internal fun Route.SaksbehandlerOppgavekoApis() {
    val oppgaveTjeneste by inject<OppgaveTjeneste>()
    val pepClient by inject<IPepClient>()
    val oppgaveKøRepository by inject<OppgaveKøRepository>()
    val requestContextService by inject<IRequestContextService>()
    val profile by inject<KoinProfile>()

    @Location("/oppgaveko")
    class getSakslister

    get { _: getSakslister ->
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
            if (pepClient.harBasisTilgang()) {
                val hentOppgaveKøer = oppgaveTjeneste.hentOppgaveKøer()
                val list = hentOppgaveKøer
                    .filter { oppgaveKø ->
                        oppgaveKø.saksbehandlere
                            .any { saksbehandler -> saksbehandler.epost == coroutineContext.idToken().getUsername().toLowerCase() }
                    }
                    .map { oppgaveKø ->
                        val sortering = SorteringDto(oppgaveKø.sortering, oppgaveKø.fomDato, oppgaveKø.tomDato)

                        OppgavekøDto(
                            id = oppgaveKø.id,
                            navn = oppgaveKø.navn,
                            behandlingTyper = oppgaveKø.filtreringBehandlingTyper,
                            fagsakYtelseTyper = oppgaveKø.filtreringYtelseTyper,
                            saksbehandlere = oppgaveKø.saksbehandlere,
                            antallBehandlinger = oppgaveKø.oppgaverOgDatoer.size,
                            sistEndret = oppgaveKø.sistEndret,
                            skjermet = oppgaveKø.skjermet,
                            sortering = sortering,
                            andreKriterier = oppgaveKø.filtreringAndreKriterierType
                        )

                    }
                call.respond(list)
            } else {
                call.respond(emptyList<OppgaveKø>())
            }
        }
    }

    @Location("/oppgaveko/saksbehandlere")
    class hentSakslistensSaksbehandlere

    get { _: hentSakslistensSaksbehandlere ->
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
                oppgaveKøRepository.hentOppgavekø(UUID.fromString(call.parameters["id"])).saksbehandlere
            )
        }
    }
}

