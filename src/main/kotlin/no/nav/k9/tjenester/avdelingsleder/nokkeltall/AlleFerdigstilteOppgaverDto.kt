package no.nav.k9.tjenester.avdelingsleder.nokkeltall

import no.nav.k9.domene.modell.BehandlingType

data class AlleFerdigstilteOppgaverDto(
    val behandlingType: BehandlingType,
    var ferdigstilteIdag: Int,
    var ferdigstilteSyvDager: Int = 0
)
