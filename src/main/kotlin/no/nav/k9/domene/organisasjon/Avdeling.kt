package no.nav.k9.domene.organisasjon

import no.nav.k9.domene.modell.OppgaveKø
import java.util.Collections

data class Avdeling(
    val id: Long,
    val avdelingEnhet: String,
    val navn: String,
    private val saksbehandlere: List<Saksbehandler>,
    private val oppgaveKø: List<OppgaveKø>
) {
    val kreverKode6: Boolean? = java.lang.Boolean.FALSE
    fun getSaksbehandlere(): List<Saksbehandler> {
        return Collections.unmodifiableList(saksbehandlere!!)
    }
    fun getOppgaveFiltrering(): List<OppgaveKø> {
        return Collections.unmodifiableList(oppgaveKø!!)
    }
    val AVDELING_DRAMMEN_ENHET = "4806"

}
