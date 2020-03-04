package no.nav.k9.domene.lager.oppgave

import no.nav.k9.domene.modell.AndreKriterierType
import java.time.LocalDateTime
import java.util.*

class OppgaveEventLogg(
    val behandlingId: Long,
    var eventType: OppgaveEventType,
    var andreKriterierType: AndreKriterierType?,
    var behandlendeEnhet: String,
    var fristTid: LocalDateTime?,
    var eksternId: UUID
)