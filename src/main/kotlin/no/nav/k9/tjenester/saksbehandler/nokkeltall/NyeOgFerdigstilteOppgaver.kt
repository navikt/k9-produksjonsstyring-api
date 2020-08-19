package no.nav.k9.tjenester.saksbehandler.nokkeltall

import no.nav.k9.domene.modell.BehandlingType
import no.nav.k9.domene.modell.FagsakYtelseType
import no.nav.k9.tjenester.saksbehandler.oppgave.Key
import java.time.LocalDate

data class NyeOgFerdigstilteOppgaver(
    val behandlingType: BehandlingType,
    val dato: LocalDate,
    val nye: MutableSet<String> = mutableSetOf(),
    val ferdigstilte: MutableSet<String> = mutableSetOf()
) {
    fun leggTilNy(uuid: String) {
        nye.add(uuid)
    }
    fun leggTilFerdigstilt(uuid: String) {
        ferdigstilte.add(uuid)
    }
}
