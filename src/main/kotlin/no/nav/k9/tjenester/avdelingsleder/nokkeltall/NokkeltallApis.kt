package no.nav.k9.tjenester.avdelingsleder.nokkeltall

import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.routing.Route

@KtorExperimentalLocationsAPI
fun Route.NokkeltallApis(
    nokkeltallTjeneste: NokkeltallTjeneste
) {
    @Location("/behandlinger-under-arbeid")
    class getAlleOppgaver

    get { _: getAlleOppgaver ->
    }

    @Location("/behandlinger-under-arbeid-historikk")
    class getAntallOppgaverPerDato

    get { _: getAntallOppgaverPerDato ->
    }

    @Location("/behandlinger-manuelt-vent-historikk")
    class getAntallOppgaverSattPåManuellVent

    get { _: getAntallOppgaverSattPåManuellVent ->
    }

    @Location("/behandlinger-forste-stonadsdag")
    class getOppgaverPerFørsteStønadsdag

    get { _: getOppgaverPerFørsteStønadsdag ->
    }
}
