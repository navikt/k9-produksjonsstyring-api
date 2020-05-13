package no.nav.k9.domene.organisasjon

import no.nav.k9.domene.modell.OppgaveKø

data class Avdeling(
    val id: Long,
    val avdelingEnhet: String,
    val navn: String,
    private val saksbehandlere: List<Saksbehandler>,
    private val oppgaveKø: List<OppgaveKø>
) {}
