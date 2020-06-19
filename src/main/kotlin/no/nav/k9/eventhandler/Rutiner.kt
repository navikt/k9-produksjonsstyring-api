package no.nav.k9.eventhandler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import no.nav.k9.domene.lager.oppgave.Oppgave
import no.nav.k9.domene.repository.OppgaveKøRepository
import no.nav.k9.domene.repository.OppgaveRepository
import no.nav.k9.domene.repository.ReservasjonRepository
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.system.measureTimeMillis


fun CoroutineScope.køOppdatertProsessor(
    channel: ReceiveChannel<UUID>,
    oppgaveKøRepository: OppgaveKøRepository,
    oppgaveRepository: OppgaveRepository,
    reservasjonRepository: ReservasjonRepository
) = launch {
    oppdatereKø(
        channel = channel,
        oppgaveKøRepository = oppgaveKøRepository,
        oppgaveRepository = oppgaveRepository,
        reservasjonRepository = reservasjonRepository
    )
}

fun CoroutineScope.oppdatereKøerMedOppgaveProsessor(
    channel: ReceiveChannel<Oppgave>,
    oppgaveKøRepository: OppgaveKøRepository,
    reservasjonRepository: ReservasjonRepository
) = launch {
    oppdatereKøerMedOppgave(
        channel = channel,
        oppgaveKøRepository = oppgaveKøRepository,
        reservasjonRepository = reservasjonRepository
    )
}

suspend fun oppdatereKø(
    channel: ReceiveChannel<UUID>,
    oppgaveKøRepository: OppgaveKøRepository,
    oppgaveRepository: OppgaveRepository,
    reservasjonRepository: ReservasjonRepository
) {
    val log = LoggerFactory.getLogger("behandleOppgave")
    for (uuid in channel) {

        val measureTimeMillis = measureTimeMillis {
            val aktiveOppgaver = oppgaveRepository.hentAktiveOppgaver()
            oppgaveKøRepository.lagre(uuid, refresh = true) { oppgaveKø ->
                oppgaveKø!!.oppgaverOgDatoer.clear()

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

suspend fun oppdatereKøerMedOppgave(
    channel: ReceiveChannel<Oppgave>,
    oppgaveKøRepository: OppgaveKøRepository,
    reservasjonRepository: ReservasjonRepository
) {
    val log = LoggerFactory.getLogger("behandleOppgave")

    val oppgaveListe = mutableListOf<Oppgave>()
    oppgaveListe.add(channel.receive())
    while (true) {
        val oppgave = channel.poll()
        if (oppgave == null) {
            val measureTimeMillis = measureTimeMillis {
                for (oppgavekø in oppgaveKøRepository.hent()) {
                    var refresh = false
                    for (o in oppgaveListe) {
                        refresh = refresh || oppgavekø.leggOppgaveTilEllerFjernFraKø(o, reservasjonRepository)
                    }
                    oppgaveKøRepository.lagre(
                        oppgavekø.id,
                        sorter = false,
                        refresh = refresh
                    ) {
                        for (o in oppgaveListe) {
                            it?.leggOppgaveTilEllerFjernFraKø(o, reservasjonRepository)
                        }
                        it!!
                    }
                }
            }
            log.info("Batch oppdaterer køer med ${oppgaveListe.size} oppgaver tok $measureTimeMillis ms")
            oppgaveListe.clear()
            oppgaveListe.add(channel.receive())
        } else {
            oppgaveListe.add(oppgave)
        }
    }
}