package no.nav.k9.integrasjon.kafka.dto

import no.nav.k9.sak.typer.AktørId
import no.nav.k9.sak.typer.JournalpostId
import java.time.LocalDateTime
import java.util.*

typealias PunsjId = UUID

data class PunsjEventDto(
    val eksternId: PunsjId,
    val journalpostId: JournalpostId,
    val eventTid: LocalDateTime,
    val aktørId: AktørId?,
    val aksjonspunktKoderMedStatusListe: MutableMap<String, String>,
    val pleietrengendeAktørId: String? = null,
    val type : String? = null,
    val ytelse : String? = null,
    val sendtInn : Boolean? = null
)
