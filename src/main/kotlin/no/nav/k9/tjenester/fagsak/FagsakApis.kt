package no.nav.k9.tjenester.fagsak

import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.post
import io.ktor.routing.Route

@KtorExperimentalLocationsAPI
fun Route.FagsakApis(
) {
    @Location("/fagsak/sok")
    class søkFagsaker

    post { _: søkFagsaker ->
    }
}