package no.nav.k9.tjenester.saksbehandler.saksliste

import io.ktor.application.call
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.withContext
import no.nav.k9.Configuration
import no.nav.k9.domene.modell.OppgaveKø
import no.nav.k9.domene.repository.OppgaveKøRepository
import no.nav.k9.integrasjon.abac.PepClient
import no.nav.k9.integrasjon.rest.RequestContextService
import no.nav.k9.tjenester.saksbehandler.IdToken
import no.nav.k9.tjenester.saksbehandler.idToken
import no.nav.k9.tjenester.saksbehandler.oppgave.OppgaveTjeneste
import java.util.*

@KtorExperimentalAPI
@KtorExperimentalLocationsAPI
internal fun Route.SaksbehandlerOppgavekoApis(
    oppgaveTjeneste: OppgaveTjeneste,
    pepClient: PepClient,
    requestContextService: RequestContextService,
    configuration: Configuration,
    oppgaveKøRepository: OppgaveKøRepository
) {
    @Location("/oppgaveko")
    class getSakslister

    get { _: getSakslister ->
        if (configuration.erLokalt) {
            val list = hentOppgavekøerLokalt(oppgaveTjeneste)
            call.respond(list)
        }else {
            val idtoken = call.idToken()
            withContext(
                requestContextService.getCoroutineContext(
                    context = coroutineContext,
                    idToken = idtoken
                )
            ) {
                val token = IdToken(idtoken.value)
                if (pepClient.harBasisTilgang(token)) {

                    val hentOppgaveKøer = oppgaveTjeneste.hentOppgaveKøer()
                    val list = hentOppgaveKøer
                        .filter { oppgaveKø -> oppgaveKø.saksbehandlere
                            .any { saksbehandler -> saksbehandler.epost == idtoken.getUsername().toLowerCase() } }
                        .map { oppgaveKø ->
                        val sortering = SorteringDto(oppgaveKø.sortering, oppgaveKø.fomDato, oppgaveKø.tomDato)

                        OppgavekøDto(
                            id = oppgaveKø.id,
                            navn = oppgaveKø.navn,
                            behandlingTyper = oppgaveKø.filtreringBehandlingTyper,
                            fagsakYtelseTyper = oppgaveKø.filtreringYtelseTyper,
                            saksbehandlere = oppgaveKø.saksbehandlere,
                            antallBehandlinger = oppgaveKø.oppgaver.size,
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
    }

    @Location("/oppgaveko/saksbehandlere")
    class hentSakslistensSaksbehandlere

    get { _: hentSakslistensSaksbehandlere ->
        call.respond(
            oppgaveKøRepository.hentOppgavekø(UUID.fromString(call.parameters["id"])).saksbehandlere
        )
    }
}

private fun hentOppgavekøerLokalt(oppgaveTjeneste: OppgaveTjeneste): List<OppgavekøDto> {
    val hentOppgaveKøer = oppgaveTjeneste.hentOppgaveKøer()
    val list = hentOppgaveKøer.map { oppgaveKø ->
        val sortering = SorteringDto(oppgaveKø.sortering, oppgaveKø.fomDato, oppgaveKø.tomDato)

        OppgavekøDto(
            id = oppgaveKø.id,
            navn = oppgaveKø.navn,
            behandlingTyper = oppgaveKø.filtreringBehandlingTyper,
            fagsakYtelseTyper = oppgaveKø.filtreringYtelseTyper,
            saksbehandlere = oppgaveKø.saksbehandlere,
            antallBehandlinger = oppgaveKø.oppgaver.size,
            sistEndret = oppgaveKø.sistEndret,
            sortering = sortering,
            skjermet = oppgaveKø.skjermet,
            andreKriterier = oppgaveKø.filtreringAndreKriterierType
        )

    }
    return list
}