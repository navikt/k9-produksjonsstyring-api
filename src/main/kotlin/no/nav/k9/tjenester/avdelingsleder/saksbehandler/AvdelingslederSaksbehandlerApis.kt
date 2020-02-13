package no.nav.k9.tjenester.avdelingsleder.saksbehandler

import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.locations.post
import io.ktor.routing.Route

@KtorExperimentalLocationsAPI
fun Route.AvdelingslederSaksbehandlerApis(
) {
    @Location("/avdelingsleder/saksbehandlere")
    class hentAvdelingensSaksbehandlere

    get { _: hentAvdelingensSaksbehandlere ->
    }

    class leggTilNySaksbehandler

    post { _: leggTilNySaksbehandler ->
    }

    @Location("/avdelingsleder/saksbehandlere/sok")
    class søkAvdelingensSaksbehandlere

    post { _: søkAvdelingensSaksbehandlere ->
    }

    @Location("/avdelingsleder/saksbehandlere/slett")
    class slettSaksbehandler

    post { _: slettSaksbehandler ->
    }
}