package no.nav.k9.tjenester.fagsak

import io.ktor.application.call
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.post
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import kotlinx.coroutines.withContext
import no.nav.k9.Configuration
import no.nav.k9.domene.modell.BehandlingStatus

import no.nav.k9.domene.modell.FagsakStatus
import no.nav.k9.domene.modell.FagsakYtelseType
import no.nav.k9.integrasjon.rest.RequestContextService
import no.nav.k9.tjenester.saksbehandler.idToken
import no.nav.k9.tjenester.saksbehandler.oppgave.OppgaveTjeneste

import java.time.LocalDate
import java.time.LocalDateTime

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

       /* val idToken = call.idToken()
        withContext(
            requestContextService.getCoroutineContext(
                context = coroutineContext,
                idToken = idToken
            )
        ) {
            call.respond(oppgaveTjeneste.søkFagsaker(søk.searchString))

        }*/

        call.respond(listOf(FagsakDto("1234",
        PersonDto("fdkshdl", "78696", "KVINNE", null), FagsakYtelseType.OMSORGSPENGER, null, LocalDateTime.now(), true
        )))
    }
}
