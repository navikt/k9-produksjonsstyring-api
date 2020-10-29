package no.nav.k9.eventhandler

import io.ktor.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import no.nav.k9.domene.lager.oppgave.Oppgave
import no.nav.k9.domene.repository.OppgaveKøRepository
import no.nav.k9.domene.repository.ReservasjonRepository
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.Executors
import kotlin.system.measureTimeMillis


@KtorExperimentalAPI
fun CoroutineScope.oppdatereKøerMedOppgaveProsessor(
    channel: ReceiveChannel<Oppgave>,
    oppgaveRefreshChannel: SendChannel<UUID>,
    statistikkRefreshChannel: SendChannel<Boolean>,
    oppgaveKøRepository: OppgaveKøRepository,
    reservasjonRepository: ReservasjonRepository
) = launch(Executors.newSingleThreadExecutor().asCoroutineDispatcher()) {
    val log = LoggerFactory.getLogger("behandleOppgave")
    val oppgaveListe = mutableListOf<Oppgave>()

    oppgaveListe.add(channel.receive())
    while (true) {
        val oppgave = channel.poll()
        if (oppgave == null) {
            val measureTimeMillis =
                oppdaterKø(
                    oppgaveKøRepository = oppgaveKøRepository,
                    oppgaveListe = oppgaveListe,
                    reservasjonRepository = reservasjonRepository,
                    oppgaveRefreshChannel = oppgaveRefreshChannel,
                    statistikkRefreshChannel = statistikkRefreshChannel
                )
            log.info("Batch oppdaterer køer med ${oppgaveListe.size} oppgaver tok $measureTimeMillis ms")
            log.info("Batch oppdaterer køer med ${oppgaveListe[0].toString()} ")
            oppgaveListe.clear()
            oppgaveListe.add(channel.receive())
        } else {
            oppgaveListe.add(oppgave)
        }
    }
}

@KtorExperimentalAPI
 suspend fun oppdaterKø(
    oppgaveKøRepository: OppgaveKøRepository,
    oppgaveListe: MutableList<Oppgave>,
    reservasjonRepository: ReservasjonRepository,
    oppgaveRefreshChannel: SendChannel<UUID>,
    statistikkRefreshChannel: SendChannel<Boolean>,
): Long {
    return measureTimeMillis {
        for (oppgavekø in oppgaveKøRepository.hentKøIdIkkeTaHensyn()) {
            oppgaveKøRepository.leggTilOppgaverTilKø(oppgavekø, oppgaveListe, reservasjonRepository)
            statistikkRefreshChannel.send(true)
        }
    }
}

