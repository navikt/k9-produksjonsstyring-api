package no.nav.k9.domene.lager.oppgave

import java.time.LocalDateTime
import java.util.*

class OppgaveEventLogg(
    private val id: Long,
    val behandlingId: Long,
    var eventType: OppgaveEventType,
    var andreKriterierType: AndreKriterierType,
    var behandlendeEnhet: String,
    var fristTid: LocalDateTime,
    var eksternId: UUID
)