package no.nav.k9.domene.lager.oppgave

import java.math.BigDecimal
import java.time.LocalDateTime

data class TilbakekrevingOppgave(
val belop: BigDecimal,
val feilutbetalingstart: LocalDateTime
)
