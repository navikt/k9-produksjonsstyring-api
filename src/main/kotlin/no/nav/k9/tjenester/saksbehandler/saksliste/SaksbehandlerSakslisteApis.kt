package no.nav.k9.tjenester.saksbehandler.saksliste

import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.routing.Route

@KtorExperimentalLocationsAPI
fun Route.SaksbehandlerSakslisteApis(
) {
    @Location("/saksbehandler/saksliste")
    class getSakslister

    get { _: getSakslister ->
    }

    @Location("/saksbehandler/saksliste/saksbehandlere")
    class hentSakslistensSaksbehandlere

    get { _: hentSakslistensSaksbehandlere ->
    }
}