package no.nav.k9.tjenester.avdelingsleder.nokkeltall

import no.nav.k9.domene.modell.BehandlingType
import no.nav.k9.domene.modell.FagsakYtelseType
import java.time.LocalDate


data class AlleOppgaverBeholdningHistorikk(
    val fagsakYtelseType: FagsakYtelseType,
    val behandlingType: BehandlingType,
    val dato: LocalDate,
    val antall: Int
)
