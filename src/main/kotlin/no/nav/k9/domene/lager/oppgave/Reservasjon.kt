package no.nav.k9.domene.lager.oppgave

import java.time.LocalDateTime

data class Reservasjon(
    var reservertTil: LocalDateTime?,
    var reservertAv: String,
    val flyttetAv: String?,
    var flyttetTidspunkt: LocalDateTime?,
    var begrunnelse: String?
)