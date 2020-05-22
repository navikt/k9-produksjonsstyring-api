package no.nav.k9.tjenester.saksbehandler.oppgave

import java.time.LocalDateTime
import java.util.*

open class DateUtil {

    fun forskyvReservasjonsDato(resDato: LocalDateTime): LocalDateTime? {
        val c = Calendar.getInstance()
        c.set(resDato.year, resDato.month.value, resDato.dayOfMonth)
        if (c.get(Calendar.DAY_OF_WEEK) == 6) {
            return resDato.plusHours(48)
        } else if (c.get(Calendar.DAY_OF_WEEK) == 7) {
            return resDato.plusHours(24)
        }
        return resDato
    }
}
