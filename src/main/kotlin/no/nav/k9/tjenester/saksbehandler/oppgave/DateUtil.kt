package no.nav.k9.tjenester.saksbehandler.oppgave

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

fun LocalDateTime.forskyvReservasjonsDato(): LocalDateTime {
    var localDate = this.toLocalDate()
    var dagerSomSkalLeggesTil = 0L
    while (localDate != LocalDate.now()) {
        if (this.dayOfWeek == DayOfWeek.SATURDAY || this.dayOfWeek == DayOfWeek.SUNDAY) {
            dagerSomSkalLeggesTil += 1L
        }
        localDate = localDate.minusDays(1)
    }

    return this.plusDays(dagerSomSkalLeggesTil)
}
