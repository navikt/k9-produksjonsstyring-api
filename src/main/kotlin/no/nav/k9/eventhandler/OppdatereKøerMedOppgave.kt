package no.nav.k9.eventhandler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import no.nav.k9.domene.lager.oppgave.Oppgave
import no.nav.k9.domene.repository.OppgaveKøRepository
import no.nav.k9.domene.repository.ReservasjonRepository
import no.nav.k9.domene.repository.StatistikkRepository
import no.nav.k9.integrasjon.k9.IK9SakService
import no.nav.k9.sak.kontrakt.behandling.BehandlingIdDto
import no.nav.k9.sak.kontrakt.behandling.BehandlingIdListe
import no.nav.k9.tjenester.saksbehandler.oppgave.OppgaveTjeneste
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import kotlin.system.measureTimeMillis


fun CoroutineScope.oppdatereKøerMedOppgaveProsessor(
    channel: ReceiveChannel<Oppgave>,
    oppgaveKøRepository: OppgaveKøRepository,
    reservasjonRepository: ReservasjonRepository,
    k9SakService: IK9SakService,
    statistikkRepository: StatistikkRepository,
    oppgaveTjeneste: OppgaveTjeneste
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
                        refresh = refresh || oppgavekø.leggOppgaveTilEllerFjernFraKø(
                            o,
                            reservasjonRepository = reservasjonRepository
                        )
                    }
                    oppgaveKøRepository.lagreIkkeTaHensyn(
                        oppgavekø.id,
                        refresh = refresh
                    ) {
                        for (o in oppgaveListe) {
                            if (o.kode6 == oppgavekø.kode6) {
                                val endring = it!!.leggOppgaveTilEllerFjernFraKø(
                                    o,
                                    reservasjonRepository = reservasjonRepository
                                )
                                if (it.tilhørerOppgaveTilKø(
                                        o,
                                        reservasjonRepository = reservasjonRepository,
                                        taHensynTilReservasjon = false
                                    )
                                ) {
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
                    oppgaveTjeneste.hentAntallOppgaver(
                        oppgavekøId = oppgavekø.id,
                        taMedReserverte = true,
                        refresh = true
                    )
                    oppgaveTjeneste.hentAntallOppgaver(
                        oppgavekøId = oppgavekø.id,
                        taMedReserverte = false,
                        refresh = true
                    )
                }
            }
            statistikkRepository.hentFerdigstilteOgNyeHistorikkMedYtelsetypeSiste4Uker(refresh = true)
            log.info("Batch oppdaterer køer med ${oppgaveListe.size} oppgaver tok $measureTimeMillis ms")
            oppgaveListe.clear()
            oppgaveListe.add(channel.receive())
        } else {
            oppgaveListe.add(oppgave)
        }
    }
}