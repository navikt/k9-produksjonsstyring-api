package no.nav.k9.domene.lager.oppgave

import java.time.LocalDateTime

data class Reservasjon(
    val id: Long,
    val oppgave: Oppgave,
    val reservertTil: LocalDateTime,
    val reservertAv: String,
    val flyttetAv: String,
    val flyttetTidspunkt: LocalDateTime,
    val begrunnelse: String
)