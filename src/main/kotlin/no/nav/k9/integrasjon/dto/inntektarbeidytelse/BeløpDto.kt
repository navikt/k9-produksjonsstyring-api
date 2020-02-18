package no.nav.k9.integrasjon.dto.inntektarbeidytelse

import java.math.BigDecimal

data class BeløpDto(
    val verdi: BigDecimal
) {

    val ZERO = BeløpDto(BigDecimal.ZERO)

    fun compareTo(annetBeløp: BeløpDto): Int {
        return verdi!!.compareTo(annetBeløp.verdi)
    }
}
