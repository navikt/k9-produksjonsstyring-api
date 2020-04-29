package no.nav.k9.tilgangskontroll

import no.nav.k9.tilgangskontroll.abac.AbacClient
import no.nav.k9.tilgangskontroll.abac.AbacRequest
import no.nav.k9.tilgangskontroll.abac.AbacResponse

open class TilgangskontrollContext(
    private val abacClient: AbacClient
) {

    fun checkAbac(request: AbacRequest): AbacResponse = abacClient.evaluate(request)
}