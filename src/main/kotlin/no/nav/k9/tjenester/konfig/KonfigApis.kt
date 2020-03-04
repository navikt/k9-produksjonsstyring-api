package no.nav.k9.tjenester.konfig

import io.ktor.application.call
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.response.respond
import io.ktor.routing.Route

@KtorExperimentalLocationsAPI
fun Route.KonfigApis(
) {
    val k9sakUrl = "https://localhost:8020"

    @Location("/k9-sak-url")
    class hentK9SakUrl

    get { _: hentK9SakUrl ->
        call.respond(Konfig(k9sakUrl))
    }
}

class Konfig(val verdi: String)