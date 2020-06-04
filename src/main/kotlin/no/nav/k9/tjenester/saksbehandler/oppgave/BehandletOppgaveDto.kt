package no.nav.k9.tjenester.saksbehandler.oppgave

import java.util.*

data class BehandletOppgaveDto(
    val behandlingId: Long,
    val saksnummer: String,
    val eksternId: UUID,
    val personnummer: String,
    val navn: String)

