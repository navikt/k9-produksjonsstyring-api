package no.nav.k9.tjenester.avdelingsleder.oppgaveko

import no.nav.k9.domene.modell.BehandlingType

data class BehandlingsTypeDto(
    val id: String,
    val behandlingType: BehandlingType,
    val checked: Boolean
)
