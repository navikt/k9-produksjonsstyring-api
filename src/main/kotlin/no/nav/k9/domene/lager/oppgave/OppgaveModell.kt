package no.nav.k9.domene.lager.oppgave

data class OppgaveModell(val oppgaver: List<Oppgave>) {
    fun sisteOppgave(): Oppgave {
        return this.oppgaver[this.oppgaver.lastIndex]
    }
}
