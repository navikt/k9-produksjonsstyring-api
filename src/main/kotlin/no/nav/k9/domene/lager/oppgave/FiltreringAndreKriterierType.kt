package no.nav.k9.domene.lager.oppgave

data class FiltreringAndreKriterierType(
    val id: Long,
    val oppgaveFiltrering: OppgaveFiltrering,
    val oppgaveFiltreringId: Long,
    val andreKriterier: String,
    val andreKriterierType: AndreKriterierType
)