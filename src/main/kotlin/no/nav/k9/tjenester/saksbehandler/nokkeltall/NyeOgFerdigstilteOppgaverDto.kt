package no.nav.k9.tjenester.saksbehandler.nokkeltall

import no.nav.k9.domene.modell.BehandlingType
import java.time.LocalDate

data class NyeOgFerdigstilteOppgaverDto(
    val behandlingType: BehandlingType,
    val dato: LocalDate,
    private val antallNyeSet: MutableSet<String> = mutableSetOf(),
    private val antallFerdigstilteSet: MutableSet<String> = mutableSetOf()
) {
    fun leggTilNy(uuid: String) {
        antallNyeSet.add(uuid)
        antallNye = antallNyeSet.size
    }
    fun leggTilFerdigstilt(uuid: String) {
        antallFerdigstilteSet.add(uuid)
        antallFerdigstilte = antallFerdigstilteSet.size
    }
    var antallNye = 0
    var antallFerdigstilte =0
}
