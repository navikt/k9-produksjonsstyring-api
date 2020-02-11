package no.nav.k9.domene.lager.oppgave

data class FiltreringYtelseType(
    val id: Long,
    val oppgaveFiltrering: OppgaveFiltrering,
    val oppgaveFiltreringId: Long,
    val fagsakYtelseTypeKode: String,
    val fagsakYtelseType: FagsakYtelseType
)
