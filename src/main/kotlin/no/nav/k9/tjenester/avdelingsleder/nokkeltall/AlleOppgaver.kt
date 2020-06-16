package no.nav.k9.tjenester.avdelingsleder.nokkeltall

import no.nav.k9.kodeverk.behandling.BehandlingType
import no.nav.k9.kodeverk.behandling.FagsakYtelseType

data class AlleOppgaver(
    val fagsakYtelseType: FagsakYtelseType,
    val behandlingType: BehandlingType,
    val tilBeslutter: Boolean,
    val antall: Long
)
