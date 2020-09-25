package no.nav.k9.eventhandler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import no.nav.k9.integrasjon.k9.IK9SakService
import no.nav.k9.sak.kontrakt.behandling.BehandlingIdDto
import no.nav.k9.sak.kontrakt.behandling.BehandlingIdListe
import java.util.*
import java.util.concurrent.Executors


fun CoroutineScope.refreshK9(
    channel: ReceiveChannel<UUID>,
    k9SakService: IK9SakService
) = launch(Executors.newSingleThreadExecutor().asCoroutineDispatcher()) {
    val oppgaveListe = mutableListOf<UUID>()
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
    oppgaveListe: MutableList<UUID>,
    k9SakService: IK9SakService
) {
    val behandlingsListe = mutableListOf<BehandlingIdDto>()
    behandlingsListe.addAll(oppgaveListe.map { BehandlingIdDto(it) }.toList())
    k9SakService.refreshBehandlinger(BehandlingIdListe(behandlingsListe))
}