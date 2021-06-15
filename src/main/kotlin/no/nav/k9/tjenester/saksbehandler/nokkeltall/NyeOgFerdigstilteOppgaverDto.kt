package no.nav.k9.tjenester.saksbehandler.nokkeltall

import no.nav.k9.domene.modell.BehandlingType
import no.nav.k9.domene.modell.FagsakYtelseType
import java.time.LocalDate

data class NyeOgFerdigstilteOppgaverDto(
    val behandlingType: BehandlingType,
    val fagsakYtelseType: FagsakYtelseType,
    val dato: LocalDate,
    val antallNye: Int,
    val antallFerdigstilte: Int,
    val antallFerdigstilteMine: Int = 0
)
