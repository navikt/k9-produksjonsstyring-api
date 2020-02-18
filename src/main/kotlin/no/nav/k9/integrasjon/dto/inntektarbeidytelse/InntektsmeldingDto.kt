package no.nav.k9.integrasjon.dto.inntektarbeidytelse

import java.time.LocalDate

data class InntektsmeldingDto(
    var arbeidsgiver: String,
    var arbeidsgiverOrgnr: String,
    var arbeidsgiverStartdato: LocalDate,
    val utsettelsePerioder: List<UtsettelsePeriodeDto>,
    val graderingPerioderDto: List<GraderingPeriodeDto>,
    var refusjonBeløpPerMnd: BeløpDto
)