package no.nav.k9.tjenester.avdelingsleder

import io.ktor.application.call
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.response.respond
import io.ktor.routing.Route

@KtorExperimentalLocationsAPI
fun Route.AvdelingslederApis(
) {
    @Location("/avdelingsleder/avdelinger")
    class getAvdelinger

    get { _: getAvdelinger ->
        call.respond(listOf(Avdeling(33, "Enhet", "Avdeling", false)))
    }
}