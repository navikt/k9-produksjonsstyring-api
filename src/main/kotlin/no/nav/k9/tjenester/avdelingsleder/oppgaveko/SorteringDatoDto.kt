package no.nav.k9.tjenester.avdelingsleder.oppgaveko

import java.time.LocalDate

data class SorteringDatoDto (
    val id: String,
    var fomDato: LocalDate?,
    var tomDato: LocalDate?
)
