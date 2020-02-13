package no.nav.k9.tjenester.avdelingsleder.nøkkeltall

import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.routing.Route

@KtorExperimentalLocationsAPI
fun Route.NøkkeltallApis(
) {
    @Location("/avdelingsleder/nokkeltall/behandlinger-under-arbeid")
    class getAlleOppgaverForAvdeling

    get { _: getAlleOppgaverForAvdeling ->
    }

    @Location("/avdelingsleder/nokkeltall/behandlinger-under-arbeid-historikk")
    class getAntallOppgaverForAvdelingPerDato

    get { _: getAntallOppgaverForAvdelingPerDato ->
    }

    @Location("/avdelingsleder/nokkeltall/behandlinger-manuelt-vent-historikk")
    class getAntallOppgaverSattPåManuellVentForAvdeling

    get { _: getAntallOppgaverSattPåManuellVentForAvdeling ->
    }

    @Location("/avdelingsleder/nokkeltall/behandlinger-forste-stonadsdag")
    class getOppgaverPerFørsteStønadsdag

    get { _: getOppgaverPerFørsteStønadsdag ->
    }
}