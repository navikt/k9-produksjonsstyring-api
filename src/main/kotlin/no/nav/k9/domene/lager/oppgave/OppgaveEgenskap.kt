package no.nav.k9.domene.lager.oppgave

data class OppgaveEgenskap(
    val id: Long,
    val oppgave: Oppgave,
    val oppgaveId: Long,
    val andreKriterierType: AndreKriterierType,
    val sisteSaksbehandlerForTotrinn: String,
    val aktiv: Boolean
)