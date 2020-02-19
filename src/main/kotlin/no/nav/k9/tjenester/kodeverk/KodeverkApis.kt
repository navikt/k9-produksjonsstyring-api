package no.nav.k9.tjenester.kodeverk

import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.routing.Route

@KtorExperimentalLocationsAPI
fun Route.KodeverkApis(
) {
    @Location("/kodeverk")
    class hentGruppertKodeliste

    get { _: hentGruppertKodeliste ->
    }
}