package no.nav.k9.eventhandler

import io.ktor.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import no.nav.k9.domene.repository.OppgaveKøRepository
import no.nav.k9.domene.repository.OppgaveRepository
import no.nav.k9.domene.repository.ReservasjonRepository
import no.nav.k9.domene.repository.SaksbehandlerRepository
import no.nav.k9.integrasjon.k9.IK9SakService
import no.nav.k9.sak.kontrakt.behandling.BehandlingIdDto
import no.nav.k9.sak.kontrakt.behandling.BehandlingIdListe
import no.nav.k9.tjenester.saksbehandler.oppgave.OppgaveTjeneste
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.Executors
import kotlin.concurrent.fixedRateTimer
import kotlin.system.measureTimeMillis


@KtorExperimentalAPI
fun CoroutineScope.køOppdatertProsessor(
    channel: ReceiveChannel<UUID>,
    oppgaveKøRepository: OppgaveKøRepository,
    oppgaveRepository: OppgaveRepository,
    oppgaveTjeneste: OppgaveTjeneste,
    reservasjonRepository: ReservasjonRepository,
    k9SakService: IK9SakService
) = launch(Executors.newSingleThreadExecutor().asCoroutineDispatcher()) {
    val log = LoggerFactory.getLogger("behandleOppgave")
    for (uuid in channel) {
        hentAlleElementerIkøSomSet(uuid, channel = channel).forEach {
            val measureTimeMillis = oppdaterKø(
                oppgaveKøRepository,
                it,
                oppgaveRepository,
                reservasjonRepository,
                oppgaveTjeneste,
                k9SakService
            )
            log.info("tok ${measureTimeMillis}ms å oppdatere kø")
        }
    }
}

@KtorExperimentalAPI
private suspend fun oppdaterKø(
    oppgaveKøRepository: OppgaveKøRepository,
    it: UUID,
    oppgaveRepository: OppgaveRepository,
    reservasjonRepository: ReservasjonRepository,
    oppgaveTjeneste: OppgaveTjeneste,
    k9SakService: IK9SakService
) {
    measureTimeMillis {
        val kø = oppgaveKøRepository.hentOppgavekø(it)
        val opprinnelige = kø.oppgaverOgDatoer
        
        // dersom den er uendret når vi skal lagre, foreta en check og eventuellt lagre på nytt inne i lås
        val aktiveOppgaver = oppgaveRepository.hentAktiveOppgaver()
            .filter { !kø.erOppgavenReservert(reservasjonRepository, it) }
        kø.oppgaverOgDatoer.clear()
        for (oppgave in aktiveOppgaver) {
            if (kø.kode6 == oppgave.kode6) {
                kø.leggOppgaveTilEllerFjernFraKø(
                    oppgave = oppgave,
                    reservasjonRepository = reservasjonRepository,
                    taHensynTilReservasjon = false
                )
            }
        }
        val behandlingsListe = mutableListOf<BehandlingIdDto>()
        oppgaveKøRepository.lagreIkkeTaHensyn(it) { oppgaveKø ->
            if (oppgaveKø!!.oppgaverOgDatoer == opprinnelige) {
                oppgaveKø.oppgaverOgDatoer = kø.oppgaverOgDatoer
            } else {
                oppgaveKø.oppgaverOgDatoer.clear()
                for (oppgave in aktiveOppgaver) {
                    if (kø.kode6 == oppgave.kode6) {
                        oppgaveKø.leggOppgaveTilEllerFjernFraKø(
                            oppgave = oppgave,
                            reservasjonRepository = reservasjonRepository,
                            taHensynTilReservasjon = false
                        )
                    }
                }
            }
            behandlingsListe.addAll(oppgaveKø.oppgaverOgDatoer.take(20).map { BehandlingIdDto(it.id) }
                .toList())
            oppgaveKø
        }
        oppgaveTjeneste.hentAntallOppgaver(oppgavekøId = it, taMedReserverte = true, refresh = true)
        oppgaveTjeneste.hentAntallOppgaver(oppgavekøId = it, taMedReserverte = false, refresh = true)
        k9SakService.refreshBehandlinger(BehandlingIdListe(behandlingsListe))
    }
}

@KtorExperimentalAPI
fun sjekkReserverteJobb(
    reservasjonRepository: ReservasjonRepository,
    saksbehandlerRepository: SaksbehandlerRepository
): Timer {
    return fixedRateTimer(
        name = "sjekkReserverteTimer", daemon = true,
        initialDelay = 0, period = 300 * 1000
    ) {
        val reservasjoner = saksbehandlerRepository.hentAlleSaksbehandlereIkkeTaHensyn().flatMap { it.reservasjoner }
        runBlocking { reservasjonRepository.hent(reservasjoner.toSet()) }
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
