package no.nav.k9.tjenester.mock

import io.ktor.application.call
import io.ktor.html.respondHtml
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.routing.Route
import io.ktor.util.KtorExperimentalAPI
import kotlinx.html.*

@KtorExperimentalAPI
@KtorExperimentalLocationsAPI
fun Route.MockGrensesnitt(
) {
    @Location("/")
    class main

    get { _: main ->
        call.respondHtml {
            head {
                title { +"Ktor: netty" }
            }
            body {
                p {
                    +"Hello from Ktor Netty engine sample application"
                }
            }
        }
    }
}