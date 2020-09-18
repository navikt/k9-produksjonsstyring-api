package no.nav.k9.eventhandler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import no.nav.k9.domene.lager.oppgave.Oppgave
import no.nav.k9.integrasjon.k9.IK9SakService
import no.nav.k9.sak.kontrakt.behandling.BehandlingIdDto
import no.nav.k9.sak.kontrakt.behandling.BehandlingIdListe
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors


fun CoroutineScope.refreshK9(
    channel: ReceiveChannel<Oppgave>,
    k9SakService: IK9SakService
) = launch(Executors.newSingleThreadExecutor().asCoroutineDispatcher()) {
    val log = LoggerFactory.getLogger("behandleOppgave")
    val oppgaveListe = mutableListOf<Oppgave>()
    oppgaveListe.add(channel.receive())
    while (true) {
        val oppgave = channel.poll()
        if (oppgave == null) {
            refreshK9(oppgaveListe, k9SakService)
            oppgaveListe.clear()
            oppgaveListe.add(channel.receive())
        } else {
            oppgaveListe.add(oppgave)
        }
    }
}

private suspend fun refreshK9(
    oppgaveListe: MutableList<Oppgave>,
    k9SakService: IK9SakService
) {
    val behandlingsListe = mutableListOf<BehandlingIdDto>()
    behandlingsListe.addAll(oppgaveListe.map { BehandlingIdDto(it.eksternId) }.toList())
    k9SakService.refreshBehandlinger(BehandlingIdListe(behandlingsListe))
}