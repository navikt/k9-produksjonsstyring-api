package no.nav.k9.domene.lager.oppgave

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class Oppgave(
    val id: Long,
    val behandlingId: Long,
    val fagsakSaksnummer: Long,
    val aktorId: Long,
    val behandlendeEnhet: String,
    val behandlingsfrist: LocalDateTime,
    val behandlingOpprettet: LocalDateTime,
    val forsteStonadsdag: LocalDate,
    val behandlingStatus: BehandlingStatus,
    val behandlingType: BehandlingType,
    val fagsakYtelseType: FagsakYtelseType,
    val aktiv: Boolean,
    val system: String,
    val oppgaveAvsluttet: LocalDateTime,
    val utfortFraAdmin: Boolean,
    val eksternId: UUID,
    val reservasjon: Reservasjon
)