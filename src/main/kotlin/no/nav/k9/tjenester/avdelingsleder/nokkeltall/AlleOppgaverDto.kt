package no.nav.k9.tjenester.avdelingsleder.nokkeltall

import no.nav.k9.domene.modell.BehandlingType
import no.nav.k9.domene.modell.FagsakYtelseType

data class AlleOppgaverDto(
    val fagsakYtelseType: FagsakYtelseType,
    val behandlingType: BehandlingType,
    val tilBehandling: Boolean,
    val antall: Int
)
