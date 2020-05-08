package no.nav.k9.eventhandler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import no.nav.k9.domene.repository.OppgaveKøRepository
import no.nav.k9.domene.repository.OppgaveRepository
import no.nav.k9.domene.repository.ReservasjonRepository
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.system.measureTimeMillis


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
val log = LoggerFactory.getLogger("behandleOppgave")
suspend fun behandleOppgave(
    channel: ReceiveChannel<UUID>,
    oppgaveKøRepository: OppgaveKøRepository,
    oppgaveRepository: OppgaveRepository,
    reservasjonRepository: ReservasjonRepository
) {
    for (uuid in channel) {
        val measureTimeMillis = measureTimeMillis {
            val aktiveOppgaver = oppgaveRepository.hentAktiveOppgaver()
            oppgaveKøRepository.lagre(uuid) { oppgaveKø ->
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
        log.info("tok ${measureTimeMillis}ms å oppdatere kø")
    }
}