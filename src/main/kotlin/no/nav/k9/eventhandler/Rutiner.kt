package no.nav.k9.eventhandler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import no.nav.k9.domene.lager.oppgave.Oppgave
import no.nav.k9.domene.repository.OppgaveKøRepository
import no.nav.k9.domene.repository.OppgaveRepository
import no.nav.k9.domene.repository.ReservasjonRepository
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.Executors
import kotlin.system.measureTimeMillis


fun CoroutineScope.køOppdatertProsessor(
    channel: ReceiveChannel<UUID>,
    oppgaveKøRepository: OppgaveKøRepository,
    oppgaveRepository: OppgaveRepository,
    reservasjonRepository: ReservasjonRepository
) = launch(Executors.newSingleThreadExecutor().asCoroutineDispatcher()) {
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
) = launch(Executors.newSingleThreadExecutor().asCoroutineDispatcher()) {
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
        hentAlleElementerIkøSomSet(uuid, channel).forEach {
            val measureTimeMillis = measureTimeMillis {
                val aktiveOppgaver = oppgaveRepository.hentAktiveOppgaver()

                //oppdatert kø utenfor lås
                // dersom den er uendret når vi skal lagre, foreta en check og eventuellt lagre på nytt inne i lås
                val oppgavekøGammel = oppgaveKøRepository.hentOppgavekø(it)
                val oppgavekøModifisert = oppgaveKøRepository.hentOppgavekø(it)
                oppgavekøModifisert.oppgaverOgDatoer.clear()
                for (oppgave in aktiveOppgaver) {
                    oppgavekøModifisert.leggOppgaveTilEllerFjernFraKø(
                        oppgave = oppgave,
                        reservasjonRepository = reservasjonRepository
                    )
                }

                oppgaveKøRepository.lagre(it) { oppgaveKø ->
                    if (oppgaveKø!! == oppgavekøGammel) {
                        oppgaveKø.oppgaverOgDatoer = oppgavekøModifisert.oppgaverOgDatoer
                    }else{
                        oppgaveKø.oppgaverOgDatoer.clear()
                        for (oppgave in aktiveOppgaver) {
                            oppgaveKø.leggOppgaveTilEllerFjernFraKø(
                                oppgave = oppgave,
                                reservasjonRepository = reservasjonRepository
                            )
                        }
                    }

                    oppgaveKø
                }
            }
            log.info("tok ${measureTimeMillis}ms å oppdatere kø")
        }
    }
}

private fun hentAlleElementerIkøSomSet(
    uuid: UUID,
    channel: ReceiveChannel<UUID>
): MutableSet<UUID> {
    val set = mutableSetOf(uuid)
    var neste = channel.poll()
    while (neste != null) {
        set.add(neste)
        neste = channel.poll()
    }
    return set
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
