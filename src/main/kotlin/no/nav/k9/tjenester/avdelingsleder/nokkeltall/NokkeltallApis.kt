package no.nav.k9.tjenester.avdelingsleder.nokkeltall

import io.ktor.application.call
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.response.respond
import io.ktor.routing.Route

@KtorExperimentalLocationsAPI
fun Route.NokkeltallApis(
    nokkeltallTjeneste: NokkeltallTjeneste
) {
    @Location("/behandlinger-under-arbeid")
    class getAlleOppgaver

    get { _: getAlleOppgaver ->
        call.respond(nokkeltallTjeneste.hentOppgaverUnderArbeid())
    }

    @Location("/behandlinger-under-arbeid-historikk")
    class getAntallOppgaverPerDato

    get { _: getAntallOppgaverPerDato ->
        call.respond(nokkeltallTjeneste.hentOppgaverPerDato())
    }

}
