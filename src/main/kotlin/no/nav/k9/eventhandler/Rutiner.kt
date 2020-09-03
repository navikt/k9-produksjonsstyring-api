package no.nav.k9.eventhandler

import io.ktor.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import no.nav.k9.domene.lager.oppgave.Oppgave
import no.nav.k9.domene.repository.OppgaveKøRepository
import no.nav.k9.domene.repository.OppgaveRepository
import no.nav.k9.domene.repository.ReservasjonRepository
import no.nav.k9.domene.repository.SaksbehandlerRepository
import no.nav.k9.integrasjon.k9.IK9SakService
import no.nav.k9.sak.kontrakt.behandling.BehandlingIdDto
import no.nav.k9.sak.kontrakt.behandling.BehandlingIdListe
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
    reservasjonRepository: ReservasjonRepository,
    saksbehandlerRepository: SaksbehandlerRepository,
    k9SakService: IK9SakService
) = launch(Executors.newSingleThreadExecutor().asCoroutineDispatcher()) {
    oppdatereKøerMedOppgave(
        channel = channel,
        oppgaveKøRepository = oppgaveKøRepository,
        reservasjonRepository = reservasjonRepository,
        saksbehandlerRepository = saksbehandlerRepository,
        k9SakService = k9SakService
    )
}

@KtorExperimentalAPI
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
                    if (oppgavekøModifisert.kode6 == oppgave.kode6) {
                        oppgavekøModifisert.leggOppgaveTilEllerFjernFraKø(
                            oppgave = oppgave,
                            reservasjonRepository = reservasjonRepository
                        )
                    }
                }

                oppgaveKøRepository.lagreIkkeTaHensyn(it) { oppgaveKø ->
                    if (oppgaveKø!! == oppgavekøGammel) {
                        oppgaveKø.oppgaverOgDatoer = oppgavekøModifisert.oppgaverOgDatoer
                    } else {
                        oppgaveKø.oppgaverOgDatoer.clear()
                        for (oppgave in aktiveOppgaver) {
                            if (oppgavekøModifisert.kode6 == oppgave.kode6) {
                                oppgaveKø.leggOppgaveTilEllerFjernFraKø(
                                    oppgave = oppgave,
                                    reservasjonRepository = reservasjonRepository
                                )
                            }
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

@KtorExperimentalAPI
suspend fun oppdatereKøerMedOppgave(
    channel: ReceiveChannel<Oppgave>,
    oppgaveKøRepository: OppgaveKøRepository,
    reservasjonRepository: ReservasjonRepository,
    saksbehandlerRepository: SaksbehandlerRepository,
    k9SakService: IK9SakService
) {
    val log = LoggerFactory.getLogger("behandleOppgave")

    val oppgaveListe = mutableListOf<Oppgave>()
    log.info("Starter rutine for oppdatering av køer")
    oppgaveListe.add(channel.receive())
    while (true) {
        val oppgave = channel.poll()
        if (oppgave == null) {
            log.info("Starter oppdatering av oppgave")
            val measureTimeMillis = measureTimeMillis {
                reservasjonRepository.fjernGamleReservasjoner(
                    saksbehandlerRepository.hentAlleSaksbehandlereIkkeTaHensyn().flatMap { it.reservasjoner }.toSet()
                )
                log.info("Fjernet gamle reservasjoner")
                for (oppgavekø in oppgaveKøRepository.hentIkkeTaHensyn()) {
                    log.info("Fjernet gamle reservasjoner")
                    var refresh = false
                    for (o in oppgaveListe) {
                        refresh = refresh || oppgavekø.leggOppgaveTilEllerFjernFraKø(o, reservasjonRepository)
                    }
                    oppgaveKøRepository.lagreIkkeTaHensyn(
                        oppgavekø.id,
                        refresh = refresh
                    ) {
                        for (o in oppgaveListe) {
                            if (o.kode6 == oppgavekø.kode6) {
                                val endring = it!!.leggOppgaveTilEllerFjernFraKø(o, reservasjonRepository)
                                if (it.tilhørerOppgaveTilKø(o, reservasjonRepository, false)) {
                                    it.nyeOgFerdigstilteOppgaver(o).leggTilNy(o.eksternId.toString())
                                }
                            }
                        }
                        it!!
                    }
                   val behandlingsListe = mutableListOf<BehandlingIdDto>()
                    behandlingsListe.addAll(oppgavekø.oppgaverOgDatoer.take(10).map { BehandlingIdDto(it.id) }.toList())
                    k9SakService.refreshBehandlinger(BehandlingIdListe(behandlingsListe))
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
