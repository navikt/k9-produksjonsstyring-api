package no.nav.k9.tjenester.saksbehandler.nøkkeltall

import no.nav.k9.domene.modell.BehandlingType
import java.time.LocalDate

class NyeOgFerdigstilteOppgaverDto(
    val behandlingType: BehandlingType,
    val antallNye: Long,
    val antallFerdigstilete: Long,
    val dato: LocalDate
)