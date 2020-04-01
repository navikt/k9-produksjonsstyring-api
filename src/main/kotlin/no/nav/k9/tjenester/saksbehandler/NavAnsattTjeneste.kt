package no.nav.k9.tjenester.saksbehandler

import no.nav.k9.tilgangskontroll.Policies
import no.nav.k9.tilgangskontroll.Tilgangskontroll
import no.nav.k9.tilgangskontroll.rsbac.DecisionEnums

open class NavAnsattTjeneste(
    private val tilgangskontroll: Tilgangskontroll
) {

    fun saksbehandlerHarTilgangTilDiskresjonskode(diskresjonskode: String): Boolean {
        return tilgangskontroll.check(Policies.tilgangTilKode6.with(diskresjonskode))
            .getDecision().decision == DecisionEnums.PERMIT
    }
}