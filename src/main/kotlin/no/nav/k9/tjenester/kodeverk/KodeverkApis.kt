package no.nav.k9.tjenester.kodeverk

import io.ktor.application.call
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.response.respond
import io.ktor.routing.Route

@KtorExperimentalLocationsAPI
fun Route.KodeverkApis(
    kodeverkTjeneste: HentKodeverkTjeneste
) {
    @Location("/kodeverk")
    class hentGruppertKodeliste

    get { _: hentGruppertKodeliste ->
        kodeverkTjeneste.hentGruppertKodeliste()?.let { call.respond(it) }
    }
}