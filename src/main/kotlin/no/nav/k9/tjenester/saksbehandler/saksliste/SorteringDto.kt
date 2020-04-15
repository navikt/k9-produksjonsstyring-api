package no.nav.k9.tjenester.saksbehandler.saksliste

import no.nav.k9.domene.lager.oppgave.KøSortering
import java.time.LocalDate

class SorteringDto(
    val sorteringType: KøSortering,
    val fra: Long?,
    val til: Long?,
    val fomDato: LocalDate,
    val tomDato: LocalDate,
    val erDynamiskPeriode: Boolean
)
