package no.nav.k9.tilgangskontroll

import no.nav.k9.tilgangskontroll.abac.AbacRequest
import no.nav.k9.tilgangskontroll.abac.AbacResponse
import java.util.*

interface TilgangskontrollContext {
    fun checkAbac(request: AbacRequest): AbacResponse
    fun hentSaksbehandlerId() : Optional<String>
    fun harSaksbehandlerRolle(rolle: String): Boolean
}