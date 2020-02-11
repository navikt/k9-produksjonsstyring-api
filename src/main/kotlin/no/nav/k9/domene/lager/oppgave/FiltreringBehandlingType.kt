package no.nav.k9.domene.lager.oppgave

data class FiltreringBehandlingType(
    val id: Long,
    val oppgaveFiltrering: OppgaveFiltrering,
    val oppgaveFiltreringId: Long,
    val behandlingTypeKode: String,
    val behandlingType: BehandlingType
)
