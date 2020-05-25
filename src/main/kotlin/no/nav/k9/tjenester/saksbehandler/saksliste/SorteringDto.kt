package no.nav.k9.tjenester.saksbehandler.saksliste

import no.nav.k9.domene.modell.KøSortering
import java.time.LocalDate

class SorteringDto(
    val sorteringType: KøSortering,
    val fomDato: LocalDate?,
    val tomDato: LocalDate?
)
