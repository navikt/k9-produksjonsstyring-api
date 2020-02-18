package no.nav.k9.integrasjon.dto

import UttakPeriodeVurderingType
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal

@JsonIgnoreProperties(ignoreUnknown = true)
class KontrollerFaktaPeriodeDto @JsonCreator constructor(
    @JsonProperty("overføringÅrsak") val overføringÅrsak: OverføringÅrsak,
    @JsonProperty("utsettelseÅrsak") val utsettelseÅrsak: UtsettelseÅrsak,
    @JsonProperty("resultat")  val resultat: UttakPeriodeVurderingType,
    @JsonProperty("arbeidstidsprosent")  val arbeidstidsprosent: BigDecimal
) {
    fun gjelderSykdom(): Boolean {
        return overføringGjelderSykdom() || utsettelseGjelderSykdom()
    }

    private fun overføringGjelderSykdom(): Boolean {
        return overføringÅrsak != null && resultat != null && overføringÅrsak.gjelderSykdom() && resultat.erOmsøktOgIkkeAvklart(resultat)
    }

    private fun utsettelseGjelderSykdom(): Boolean {
        return utsettelseÅrsak != null && resultat != null && utsettelseÅrsak.gjelderSykdom() && resultat.erOmsøktOgIkkeAvklart(resultat)
    }
}