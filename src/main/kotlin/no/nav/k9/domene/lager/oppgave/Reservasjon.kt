package no.nav.k9.domene.lager.oppgave

import java.time.LocalDateTime
import java.util.*

data class Reservasjon(
    var reservertTil: LocalDateTime?,
    var reservertAv: String?,
    val flyttetAv: String?,
    var flyttetTidspunkt: LocalDateTime?,
    var begrunnelse: String?,
    val oppgave: UUID
) {
    //Skriv om til å hente direkte slik det er nå kommer det dobbelt opp
    fun erAktiv(): Boolean {
        return reservertTil !=null &&  reservertTil!!.isAfter(LocalDateTime.now())
    }
}