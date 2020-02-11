package no.nav.k9.domene.lager.oppgave

enum class OppgaveEventType {
    OPPRETTET, LUKKET, VENT, MANU_VENT, GJENAPNET;

    fun erÅpningsevent(): Boolean {
        return this == OPPRETTET || this == GJENAPNET
    }
}
