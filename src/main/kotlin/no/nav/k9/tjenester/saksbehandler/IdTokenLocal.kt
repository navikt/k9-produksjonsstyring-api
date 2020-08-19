package no.nav.k9.tjenester.saksbehandler

import no.nav.k9.domene.oppslag.Ident

data class IdTokenLocal(
    override val value: String = "",
    override val ident: Ident = Ident(
        "saksbehandler@nav.no"
    )
) : IIdToken {
    override val jwt = null
    override fun getName(): String = "saksbehandler@nav.no"
    override fun getUsername(): String = "saksbehandler@nav.no"
    override fun kanBehandleKode6(): Boolean = true
    override fun kanBehandleKode7(): Boolean = true
    override fun kanBehandleEgneAnsatte(): Boolean = true
    override fun erOppgavebehandler(): Boolean = true
}