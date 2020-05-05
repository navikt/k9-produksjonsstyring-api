package no.nav.k9.tjenester.saksbehandler.saksliste

import io.ktor.application.call
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.response.respond
import io.ktor.routing.Route
import kotlinx.coroutines.withContext
import no.nav.k9.domene.modell.OppgaveKø
import no.nav.k9.domene.modell.Saksbehandler
import no.nav.k9.integrasjon.abac.PepClient
import no.nav.k9.integrasjon.rest.RequestContextService
import no.nav.k9.tjenester.saksbehandler.IdToken
import no.nav.k9.tjenester.saksbehandler.idToken
import no.nav.k9.tjenester.saksbehandler.oppgave.OppgaveTjeneste

@KtorExperimentalLocationsAPI
internal fun Route.SaksbehandlerSakslisteApis(
    oppgaveTjeneste: OppgaveTjeneste,
    pepClient: PepClient,
    requestContextService: RequestContextService
) {
    @Location("/saksliste")
    class getSakslister

    get { _: getSakslister ->
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
                val list = hentOppgaveKøer.map { oppgaveKø ->
                    val sortering = SorteringDto(oppgaveKø.sortering, oppgaveKø.fomDato, oppgaveKø.tomDato)

                    OppgavekøDto(
                        id = oppgaveKø.id,
                        navn = oppgaveKø.navn,
                        behandlingTyper = oppgaveKø.filtreringBehandlingTyper,
                        fagsakYtelseTyper = oppgaveKø.filtreringYtelseTyper,
                        saksbehandlere = listOf(Saksbehandler("alexaban", "Sara Saksbehandler", "alexaban@nav.no")),
                        antallBehandlinger = 1000,
                        sistEndret = oppgaveKø.sistEndret,
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

    @Location("/saksliste/saksbehandlere")
    class hentSakslistensSaksbehandlere

    get { _: hentSakslistensSaksbehandlere ->
        call.respond(
            listOf(Saksbehandler("8ewer89uf", "SaksbehandlerEllen", "ellen@nav.no"))
        )
    }
}
