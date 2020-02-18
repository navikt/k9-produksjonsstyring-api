package no.nav.k9.integrasjon.dto.ytelsefordeling

import no.nav.k9.integrasjon.dto.periode.PeriodeDto

class AnnenforelderHarRettDto(
    var annenforelderHarRett: Boolean,
    var begrunnelse: String,
    var annenforelderHarRettPerioder: List<PeriodeDto> = emptyList()
)