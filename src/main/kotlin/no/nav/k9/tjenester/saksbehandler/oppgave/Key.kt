package no.nav.k9.tjenester.saksbehandler.oppgave

import no.nav.k9.domene.modell.BehandlingType
import no.nav.k9.domene.modell.FagsakYtelseType

data class Key(
    val behandlingType: BehandlingType,
    val fagsakYtelseType: FagsakYtelseType
)
