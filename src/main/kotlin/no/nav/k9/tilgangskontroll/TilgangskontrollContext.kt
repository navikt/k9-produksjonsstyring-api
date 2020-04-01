package no.nav.k9.tilgangskontroll

import no.nav.k9.auth.IdToken
import no.nav.k9.ldap.LDAPService
import no.nav.k9.tilgangskontroll.abac.AbacClient
import no.nav.k9.tilgangskontroll.abac.AbacRequest
import no.nav.k9.tilgangskontroll.abac.AbacResponse
import java.util.*

open class TilgangskontrollContext(
    private val abacClient: AbacClient,
    private val idToken: IdToken,
    private val ldap: LDAPService
) {

    fun checkAbac(request: AbacRequest): AbacResponse = abacClient.evaluate(request)
    fun hentSaksbehandlerId(): String? = idToken.getId()
    fun harSaksbehandlerRolle(rolle: String) = hentSaksbehandlerRoller().contains(rolle.toLowerCase())

    private fun hentSaksbehandlerRoller(): List<String> =
        hentSaksbehandlerId()?.let { ldap.hentRollerForVeileder(it) }!!.map { it.toLowerCase() }
}