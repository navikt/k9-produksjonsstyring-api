package no.nav.k9.tjenester.avdelingsleder.nokkeltall

import no.nav.k9.kodeverk.behandling.BehandlingType
import no.nav.k9.kodeverk.behandling.FagsakYtelseType

data class AlleOppgaverDto(
    val fagsakYtelseType: FagsakYtelseType,
    val behandlingType: BehandlingType,
    val tilBehandling: Boolean,
    val antall: Long
)
