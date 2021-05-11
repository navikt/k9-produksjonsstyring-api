package no.nav.k9.tjenester.fagsak

import io.ktor.application.call
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.post
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.util.KtorExperimentalAPI
import no.nav.k9.Configuration
import no.nav.k9.KoinProfile
import no.nav.k9.domene.modell.BehandlingStatus
import no.nav.k9.domene.modell.BehandlingType
import no.nav.k9.domene.modell.FagsakYtelseType
import no.nav.k9.integrasjon.rest.RequestContextService
import no.nav.k9.tjenester.saksbehandler.oppgave.OppgaveDto
import no.nav.k9.tjenester.saksbehandler.oppgave.OppgaveStatusDto
import no.nav.k9.tjenester.saksbehandler.oppgave.OppgaveTjeneste
import no.nav.k9.tjenester.saksbehandler.oppgave.SokeResultatDto
import org.koin.ktor.ext.inject
import java.time.LocalDateTime
import java.util.*

@KtorExperimentalAPI
@KtorExperimentalLocationsAPI
internal fun Route.FagsakApis() {
    val oppgaveTjeneste by inject<OppgaveTjeneste>()
    val configuration by inject<Configuration>()
    val requestContextService by inject<RequestContextService>()

    @Location("/sok")
    class søkFagsaker
    post { _: søkFagsaker ->
        if (KoinProfile.LOCAL == configuration.koinProfile()) {
            call.respond(SokeResultatDto(true, null, mutableListOf()))
        } else {
            requestContextService.withRequestContext(call) {
                val søk = call.receive<QueryString>()
                call.respond(oppgaveTjeneste.søkFagsaker(søk.searchString))
            }
        }
    }

    @Location("/aktoerid-sok")
    class søkFagsakerMedAktørId
    post { _: søkFagsakerMedAktørId ->
        requestContextService.withRequestContext(call) {
            val param = call.receive<AktoerIdDto>()
            call.respond(oppgaveTjeneste.finnOppgaverBasertPåAktørId(param.aktoerId))
        }
    }
}
