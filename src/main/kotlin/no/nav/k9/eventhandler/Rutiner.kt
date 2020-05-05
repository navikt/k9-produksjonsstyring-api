package no.nav.k9.eventhandler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import no.nav.k9.domene.lager.oppgave.Oppgave
import no.nav.k9.domene.repository.OppgaveKøRepository


fun CoroutineScope.launchOppgaveOppdatertProcessor(
    channel: ReceiveChannel<Oppgave>,
    oppgaveKøRepository: OppgaveKøRepository
) = launch {
    behandleOppgave(channel, oppgaveKøRepository)
}

suspend fun behandleOppgave(
    channel: ReceiveChannel<Oppgave>,
    oppgaveKøRepository: OppgaveKøRepository
) {
    for (oppgave in channel) {
        val oppgavekøer = oppgaveKøRepository.hent()
        for (oppgavekø in oppgavekøer) {
            oppgaveKøRepository.lagre(oppgavekø.id) { forrige ->
                forrige?.leggOppgaveTilEllerFjernFraKø(oppgave)
                forrige!!
            }
        }
    }
}