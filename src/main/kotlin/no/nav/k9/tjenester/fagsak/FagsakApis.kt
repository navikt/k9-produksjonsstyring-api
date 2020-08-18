package no.nav.k9.tjenester.fagsak

import io.ktor.application.call
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.post
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.withContext
import no.nav.k9.Configuration
import no.nav.k9.KoinProfile
import no.nav.k9.integrasjon.rest.IRequestContextService
import no.nav.k9.tjenester.saksbehandler.idToken
import no.nav.k9.tjenester.saksbehandler.oppgave.OppgaveTjeneste
import org.koin.ktor.ext.inject

@KtorExperimentalAPI
@KtorExperimentalLocationsAPI
internal fun Route.FagsakApis() {
    val oppgaveTjeneste by inject<OppgaveTjeneste>()
    val configuration by inject<Configuration>()
    val requestContextService by inject<IRequestContextService>()
    @Location("/sok")
    class søkFagsaker

    post { _: søkFagsaker ->
        val søk = call.receive<QueryString>()
        if (KoinProfile.LOCAL == configuration.koinProfile()) {
            call.respond(emptyList<FagsakDto>())
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
