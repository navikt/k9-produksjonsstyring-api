package no.nav.k9.domene.organisasjon

import no.nav.k9.domene.lager.oppgave.OppgaveFiltrering

import java.util.Collections

class Avdeling {

    val id: Long? = null

    val avdelingEnhet: String? = null

    val navn: String? = null

    private val saksbehandlere: List<Saksbehandler>? = null

    private val oppgaveFiltrering: List<OppgaveFiltrering>? = null

    val kreverKode6: Boolean? = java.lang.Boolean.FALSE

    fun getSaksbehandlere(): List<Saksbehandler> {
        return Collections.unmodifiableList(saksbehandlere!!)
    }

    fun getOppgaveFiltrering(): List<OppgaveFiltrering> {
        return Collections.unmodifiableList(oppgaveFiltrering!!)
    }

    companion object {

        val AVDELING_DRAMMEN_ENHET = "4806"
    }
}
