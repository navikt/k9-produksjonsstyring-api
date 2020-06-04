package no.nav.k9.domene.lager.oppgave

import no.nav.k9.sak.typer.AktørId
import java.util.*

data class BehandletOppgave(
    val behandlingId: Long,
    val saksnummer: String,
    val eksternId: UUID,
    val aktørId: String
)


