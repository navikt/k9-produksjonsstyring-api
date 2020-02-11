package no.nav.k9.domene.organisasjon

import no.nav.k9.domene.lager.oppgave.OppgaveFiltrering
import java.util.*

class Saksbehandler(val saksbehandlerIdent: String) {
    val id: Long? = null

    private val avdelinger = ArrayList<Avdeling>()

    private val oppgaveFiltreringer = ArrayList<OppgaveFiltrering>()

    fun getAvdelinger(): List<Avdeling> {
        return avdelinger
    }

    fun leggTilAvdeling(avdeling: Avdeling) {
        avdelinger.add(avdeling)
    }

    fun fjernAvdeling(avdeling: Avdeling) {
        avdelinger.remove(avdeling)
    }

    fun getOppgaveFiltreringer(): List<OppgaveFiltrering> {
        return Collections.unmodifiableList(oppgaveFiltreringer)
    }
}
