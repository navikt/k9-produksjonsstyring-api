package no.nav.k9.integrasjon.dto.ytelsefordeling

import no.nav.k9.integrasjon.dto.periode.PeriodeDto
import java.time.LocalDate

class YtelseFordelingDto(
    var ikkeOmsorgPerioder: List<PeriodeDto>,
    var aleneOmsorgPerioder: List<PeriodeDto>,
    var annenforelderHarRettDtoDto: AnnenforelderHarRettDto,
    var endringsdato: LocalDate,
    var gjeldendeDekningsgrad: Int = 0,
    var førsteUttaksdato: LocalDate = TODO()
)