package no.nav.k9.tjenester.fagsak

import io.ktor.application.call
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.post
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.k9.Configuration
import no.nav.k9.integrasjon.rest.RequestContextService
import no.nav.k9.tjenester.saksbehandler.idToken
import no.nav.k9.tjenester.saksbehandler.oppgave.OppgaveTjeneste

@KtorExperimentalLocationsAPI
internal fun Route.FagsakApis(
    oppgaveTjeneste: OppgaveTjeneste,
    configuration: Configuration,
    requestContextService: RequestContextService
) {
    @Location("/sok")
    class søkFagsaker

    post { _: søkFagsaker ->
        val søk = call.receive<QueryString>()
        if (configuration.erLokalt) {
            withContext(
                Dispatchers.Unconfined
            ) {
                call.respond(oppgaveTjeneste.søkFagsaker("Saksnummer"))
            }
        } else {
            val idToken = call.idToken()
            withContext(
                requestContextService.getCoroutineContext(
                    context = coroutineContext,
                    idToken = idToken
                )
            ) {
                call.respond(oppgaveTjeneste.søkFagsaker(søk.searchString))
            }
        }
    }
}
