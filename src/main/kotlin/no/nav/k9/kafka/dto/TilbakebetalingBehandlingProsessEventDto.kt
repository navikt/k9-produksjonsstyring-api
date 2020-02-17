package no.nav.k9.kafka.dto

import java.math.BigDecimal
import java.time.LocalDate

data class TilbakebetalingBehandlingProsessEventDto (
    val førsteFeilutbetaling: LocalDate,
    val feilutbetaltBeløp: BigDecimal)