package no.nav.k9.integrasjon.kafka.dto

import no.nav.k9.sak.typer.JournalpostId
import java.time.LocalDateTime
import java.util.*

typealias PunsjId = UUID

data class PunsjEventDto(
    val eksternId: PunsjId,
    val journalpostId: JournalpostId,
    val eventTid: LocalDateTime
)