package no.nav.k9.tjenester.konfig

import io.ktor.application.call
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.response.respond
import io.ktor.routing.Route
import no.nav.k9.Configuration
import no.nav.k9.KoinProfile
import org.koin.ktor.ext.inject

@KtorExperimentalLocationsAPI
fun Route.KonfigApis() {
    val configuration by inject<Configuration>()
    val k9sakUrlDev = "https://app-q1.adeo.no/k9/web"
    val k9sakUrlProd = "https://app.adeo.no/k9/web"
    val sseUrlDev = "https://k9-los-oidc-auth-proxy.nais.preprod.local/api/k9-los-api/sse"
    val sseUrlProd = "https://k9-los-oidc-auth-proxy.nais.adeo.no/api/k9-los-api/sse"
    val sseUrlLocal = "api/sse"

    @Location("/k9-sak-url")
    class hentK9SakUrl

    get { _: hentK9SakUrl ->
        if (KoinProfile.PREPROD == configuration.koinProfile()) call.respond(Konfig(k9sakUrlDev)) else call.respond(Konfig(k9sakUrlProd))
    }

    @Location("/sse-url")
    class hentSseUrl

    get { _: hentSseUrl ->
        when {
            configuration.koinProfile() == KoinProfile.PROD -> {
                call.respond(Konfig(sseUrlProd))
            }
            KoinProfile.PREPROD == configuration.koinProfile() -> {
                call.respond(Konfig(sseUrlDev))
            }
            else -> {
                call.respond(Konfig(sseUrlLocal))
            }
        }
    }
}

class Konfig(val verdi: String)
