package no.nav.k9.tjenester.saksbehandler.nokkeltall

import no.nav.k9.domene.modell.BehandlingType
import java.time.LocalDate

data class NyeOgFerdigstilteOppgaverDto(
    val behandlingType: BehandlingType,
    val antallNye: Long,
    val antallFerdigstilte: Long,
    val dato: LocalDate
)
