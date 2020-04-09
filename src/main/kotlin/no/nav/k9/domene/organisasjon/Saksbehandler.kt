package no.nav.k9.domene.organisasjon

import no.nav.k9.domene.modell.OppgaveKø
import java.util.*

class Saksbehandler(val saksbehandlerIdent: String) {
    val id: Long? = null

    private val avdelinger = ArrayList<Avdeling>()

    private val oppgaveFiltreringer = ArrayList<OppgaveKø>()

    fun getAvdelinger(): List<Avdeling> {
        return avdelinger
    }

    fun leggTilAvdeling(avdeling: Avdeling) {
        avdelinger.add(avdeling)
    }

    fun fjernAvdeling(avdeling: Avdeling) {
        avdelinger.remove(avdeling)
    }

    fun getOppgaveFiltreringer(): List<OppgaveKø> {
        return Collections.unmodifiableList(oppgaveFiltreringer)
    }
}
