package no.nav.k9.eventhandler

import io.ktor.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import no.nav.k9.domene.lager.oppgave.Oppgave
import no.nav.k9.domene.modell.OppgaveKø
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


@KtorExperimentalAPI
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

    oppgaveListe.add(channel.receive())
    while (true) {
        val oppgave = channel.poll()
        if (oppgave == null) {
            val measureTimeMillis =
                oppdaterKø(
                    oppgaveKøRepository,
                    oppgaveListe,
                    reservasjonRepository,
                    k9SakService,
                    oppgaveTjeneste,
                    statistikkRepository
                )
            log.info("Batch oppdaterer køer med ${oppgaveListe.size} oppgaver tok $measureTimeMillis ms")
            oppgaveListe.clear()
            oppgaveListe.add(channel.receive())
        } else {
            oppgaveListe.add(oppgave)
        }
    }
}

@KtorExperimentalAPI
private suspend fun oppdaterKø(
    oppgaveKøRepository: OppgaveKøRepository,
    oppgaveListe: MutableList<Oppgave>,
    reservasjonRepository: ReservasjonRepository,
    k9SakService: IK9SakService,
    oppgaveTjeneste: OppgaveTjeneste,
    statistikkRepository: StatistikkRepository
): Long {
    return measureTimeMillis {
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
                for (oppgave in oppgaveListe) {
                    if (oppgave.kode6 == oppgavekø.kode6) {
                        it!!.leggOppgaveTilEllerFjernFraKø(
                            oppgave,
                            reservasjonRepository = reservasjonRepository
                        )
                        if (it.tilhørerOppgaveTilKø(
                                oppgave,
                                reservasjonRepository = reservasjonRepository,
                                taHensynTilReservasjon = true
                            )
                        ) {
                            it.nyeOgFerdigstilteOppgaver(oppgave).leggTilNy(oppgave.eksternId.toString())
                        }
                    }
                }
                it!!
            }
            refreshK9(oppgavekø, k9SakService)
            refreshHentAntallOppgaver(oppgaveTjeneste, oppgavekø)
            statistikkRepository.hentFerdigstilteOgNyeHistorikkMedYtelsetypeSiste8Uker(refresh = true)
        }
    }
}

private suspend fun refreshK9(
    oppgavekø: OppgaveKø,
    k9SakService: IK9SakService
) {
    val behandlingsListe = mutableListOf<BehandlingIdDto>()
    behandlingsListe.addAll(oppgavekø.oppgaverOgDatoer.take(20).map { BehandlingIdDto(it.id) }.toList())
    k9SakService.refreshBehandlinger(BehandlingIdListe(behandlingsListe))
}

@KtorExperimentalAPI
private suspend fun refreshHentAntallOppgaver(
    oppgaveTjeneste: OppgaveTjeneste,
    oppgavekø: OppgaveKø
) {
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
