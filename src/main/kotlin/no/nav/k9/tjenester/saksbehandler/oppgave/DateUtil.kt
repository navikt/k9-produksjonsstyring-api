package no.nav.k9.tjenester.saksbehandler.oppgave

import java.time.DayOfWeek
import java.time.LocalDateTime

fun LocalDateTime.forskyvReservasjonsDato(): LocalDateTime {
    if (this.dayOfWeek == DayOfWeek.SATURDAY) {
        return this.plusHours(48)
    } else if (this.dayOfWeek == DayOfWeek.SUNDAY) {
        return this.plusHours(24)
    }
    return this
}
