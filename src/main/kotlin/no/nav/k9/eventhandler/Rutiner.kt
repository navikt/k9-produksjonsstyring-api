package no.nav.k9.eventhandler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import no.nav.k9.domene.lager.oppgave.Oppgave
import no.nav.k9.domene.repository.OppgaveKøRepository
import no.nav.k9.domene.repository.OppgaveRepository
import no.nav.k9.domene.repository.ReservasjonRepository
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
    reservasjonRepository: ReservasjonRepository,
    k9SakService: IK9SakService
) = launch(Executors.newSingleThreadExecutor().asCoroutineDispatcher()) {
    val log = LoggerFactory.getLogger("behandleOppgave")
    for (uuid in channel) {
        hentAlleElementerIkøSomSet(uuid, channel = channel).forEach {
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
                val behandlingsListe = mutableListOf<BehandlingIdDto>()
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
                    behandlingsListe.addAll(oppgaveKø.oppgaverOgDatoer.take(20).map { BehandlingIdDto(it.id) }.toList())
                    oppgaveKø
                }
                k9SakService
                    .refreshBehandlinger(BehandlingIdListe(behandlingsListe))
            }
            log.info("tok ${measureTimeMillis}ms å oppdatere kø")
        }
    }
}

fun CoroutineScope.oppdatereKøerMedOppgaveProsessor(
    channel: ReceiveChannel<Oppgave>,
    oppgaveKøRepository: OppgaveKøRepository,
    reservasjonRepository: ReservasjonRepository,
    k9SakService: IK9SakService
) = launch(Executors.newSingleThreadExecutor().asCoroutineDispatcher()) {
    val log = LoggerFactory.getLogger("behandleOppgave")
    val oppgaveListe = mutableListOf<Oppgave>()
    log.info("Starter rutine for oppdatering av køer")
    oppgaveListe.add(channel.receive())
    while (true) {
        val oppgave = channel.poll()
        if (oppgave == null) {
            val measureTimeMillis = measureTimeMillis {
                for (oppgavekø in oppgaveKøRepository.hentIkkeTaHensyn()) {
                    var refresh = false
                    for (o in oppgaveListe) {
                        refresh = refresh || oppgavekø.leggOppgaveTilEllerFjernFraKø(o,
                            reservasjonRepository = reservasjonRepository
                        )
                    }
                    oppgaveKøRepository.lagreIkkeTaHensyn(
                        oppgavekø.id,
                        refresh = refresh
                    ) {
                        for (o in oppgaveListe) {
                            if (o.kode6 == oppgavekø.kode6) {
                                val endring = it!!.leggOppgaveTilEllerFjernFraKø(o,
                                    reservasjonRepository = reservasjonRepository
                                )
                                if (it.tilhørerOppgaveTilKø(o,
                                        reservasjonRepository = reservasjonRepository,
                                        taHensynTilReservasjon = false
                                    )) {
                                    it.nyeOgFerdigstilteOppgaver(o).leggTilNy(o.eksternId.toString())
                                }
                            }
                        }
                        it!!
                    }
                    val behandlingsListe = mutableListOf<BehandlingIdDto>()
                    behandlingsListe.addAll(oppgavekø.oppgaverOgDatoer.take(20).map { BehandlingIdDto(it.id) }.toList())
                    k9SakService
                        .refreshBehandlinger(BehandlingIdListe(behandlingsListe))
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
           
            val measureTimeMillis = measureTimeMillis {
                val behandlingsListe = mutableListOf<BehandlingIdDto>()
                behandlingsListe.addAll(oppgaveListe.map { BehandlingIdDto(it.eksternId) }.toList())
                k9SakService
                    .refreshBehandlinger(BehandlingIdListe(behandlingsListe))
            }
            
            log.info("Refresh oppdaterer køer med ${oppgaveListe.size} oppgaver tok $measureTimeMillis ms")
            oppgaveListe.clear()
            oppgaveListe.add(channel.receive())
        } else {
            oppgaveListe.add(oppgave)
        }
    }
}

fun hentAlleElementerIkøSomSet(
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

