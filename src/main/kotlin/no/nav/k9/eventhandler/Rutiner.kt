package no.nav.k9.eventhandler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import no.nav.k9.domene.lager.oppgave.Oppgave
import no.nav.k9.domene.repository.OppgaveKøRepository
import no.nav.k9.domene.repository.ReservasjonRepository


fun CoroutineScope.launchOppgaveOppdatertProcessor(
    channel: ReceiveChannel<Oppgave>,
    oppgaveKøRepository: OppgaveKøRepository,
    reservasjonRepository: ReservasjonRepository
) = launch {
    behandleOppgave(channel, oppgaveKøRepository, reservasjonRepository)
}

suspend fun behandleOppgave(
    channel: ReceiveChannel<Oppgave>,
    oppgaveKøRepository: OppgaveKøRepository,
    reservasjonRepository: ReservasjonRepository
) {
    for (oppgave in channel) {
        for (oppgavekø in oppgaveKøRepository.hent()) {
            oppgaveKøRepository.lagre(oppgavekø.id) { forrige ->
                forrige?.leggOppgaveTilEllerFjernFraKø(oppgave, reservasjonRepository)
                forrige!!
            }
        }
    }
}