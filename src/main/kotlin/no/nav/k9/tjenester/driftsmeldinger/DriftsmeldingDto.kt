package no.nav.k9.tjenester.driftsmeldinger

import java.time.LocalDateTime
import java.util.*

data class DriftsmeldingDto(
    val id: UUID,
    val melding: String,
    val dato: LocalDateTime,
    val aktiv: Boolean
)

