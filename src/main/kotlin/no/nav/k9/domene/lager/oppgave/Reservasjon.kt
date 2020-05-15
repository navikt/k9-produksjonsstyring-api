package no.nav.k9.domene.lager.oppgave

import no.nav.k9.domene.repository.ReservasjonRepository
import java.time.LocalDateTime
import java.util.*

data class Reservasjon(
    var reservertTil: LocalDateTime?,
    var reservertAv: String?,
    val flyttetAv: String?,
    var flyttetTidspunkt: LocalDateTime?,
    var begrunnelse: String?,
    var aktiv: Boolean = true,
    val oppgave: UUID
) {
    fun erAktiv(reservasjonRepository: ReservasjonRepository): Boolean {
        
        return if (aktiv && reservertTil!!.isAfter(LocalDateTime.now())) {
            true
        } else {
            if (aktiv) {
                reservasjonRepository.lagre(oppgave) {
                    it!!.aktiv = false
                    it
                }
                aktiv = false
            }
            false
        }
    }
}