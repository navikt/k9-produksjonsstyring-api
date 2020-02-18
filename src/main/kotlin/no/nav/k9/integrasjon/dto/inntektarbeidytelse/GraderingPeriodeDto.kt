package no.nav.k9.integrasjon.dto.inntektarbeidytelse

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.LocalDate

data class GraderingPeriodeDto(
    @JsonProperty("fom")
    val fom: LocalDate,
    @JsonProperty("tom")
    val tom: LocalDate,
    @JsonProperty("arbeidsprosent")
    val arbeidsprosent: BigDecimal
)