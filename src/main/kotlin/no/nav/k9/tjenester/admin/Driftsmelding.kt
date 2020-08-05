package no.nav.k9.tjenester.admin

import java.time.LocalDateTime
import java.util.*

data class Driftsmelding(
    val id: UUID = UUID.randomUUID(),
    val melding: String,
    val dato: LocalDateTime = LocalDateTime.now(),
    val aktiv: Boolean = false
)
