package no.nav.k9.tjenester.fagsak

import io.ktor.application.call
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.post
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import no.nav.k9.domene.modell.BehandlingStatus

import no.nav.k9.domene.modell.FagsakStatus
import no.nav.k9.domene.modell.FagsakYtelseType
import no.nav.k9.tjenester.saksbehandler.oppgave.OppgaveTjeneste

import java.time.LocalDate
import java.time.LocalDateTime

@KtorExperimentalLocationsAPI
fun Route.FagsakApis(
    oppgaveTjeneste: OppgaveTjeneste
) {
    @Location("/sok")
    class søkFagsaker

    post { _: søkFagsaker ->
        val søk = call.receive<QueryString>()
        call.respond(oppgaveTjeneste.søkFagsaker(søk.searchString))
    }
}
