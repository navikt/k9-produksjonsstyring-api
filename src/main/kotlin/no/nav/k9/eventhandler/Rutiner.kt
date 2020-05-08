package no.nav.k9.eventhandler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import no.nav.k9.domene.repository.OppgaveKøRepository
import no.nav.k9.domene.repository.OppgaveRepository
import no.nav.k9.domene.repository.ReservasjonRepository
import java.util.*


fun CoroutineScope.launchOppgaveOppdatertProcessor(
    channel: ReceiveChannel<UUID>,
    oppgaveKøRepository: OppgaveKøRepository,
    oppgaveRepository: OppgaveRepository,
    reservasjonRepository: ReservasjonRepository
) = launch {
    behandleOppgave(
        channel = channel,
        oppgaveKøRepository = oppgaveKøRepository,
        oppgaveRepository = oppgaveRepository,
        reservasjonRepository = reservasjonRepository
    )
}

suspend fun behandleOppgave(
    channel: ReceiveChannel<UUID>,
    oppgaveKøRepository: OppgaveKøRepository,
    oppgaveRepository: OppgaveRepository,
    reservasjonRepository: ReservasjonRepository
) {
    for (uuid in channel) {

        val aktiveOppgaver = oppgaveRepository.hentAktiveOppgaver()
        oppgaveKøRepository. lagre(uuid) { oppgaveKø ->
            oppgaveKø!!.oppgaver.clear()
            for (oppgave in aktiveOppgaver) {
                oppgaveKø.leggOppgaveTilEllerFjernFraKø(
                    oppgave = oppgave,
                    reservasjonRepository = reservasjonRepository
                )
            }
            oppgaveKø
        }
    }
}