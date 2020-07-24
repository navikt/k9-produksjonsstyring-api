package no.nav.k9.tjenester.saksbehandler.oppgave

import java.time.LocalDateTime

data class ReservasjonDto (
    val reservertTil: LocalDateTime?,
    var reservertAv: String?,
    val flyttetAv: String?,
    var flyttetTidspunkt: LocalDateTime?,
    var begrunnelse: String?
)
