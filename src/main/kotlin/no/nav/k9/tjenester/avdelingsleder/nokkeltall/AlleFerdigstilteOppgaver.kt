package no.nav.k9.tjenester.avdelingsleder.nokkeltall

import no.nav.k9.domene.modell.BehandlingType
import java.time.LocalDate

data class AlleFerdigstilteOppgaver(
    val behandlingType: BehandlingType,
    val dato: LocalDate,
    var antall: Int
)
