package no.nav.k9.eventhandler

import io.ktor.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import no.nav.k9.domene.modell.OppgaveKø
import no.nav.k9.domene.repository.OppgaveKøRepository
import no.nav.k9.domene.repository.StatistikkRepository
import no.nav.k9.tjenester.saksbehandler.oppgave.OppgaveTjeneste
import no.nav.k9.tjenester.sse.log
import java.util.concurrent.Executors

@KtorExperimentalAPI
fun CoroutineScope.oppdaterStatistikk(
    channel: ReceiveChannel<Boolean>,
    statistikkRepository: StatistikkRepository,
    oppgaveTjeneste: OppgaveTjeneste,
    oppgaveKøRepository: OppgaveKøRepository

) = launch(Executors.newSingleThreadExecutor().asCoroutineDispatcher()) {
    while (true) {
        try {
            channel.receive()
            oppgaveKøRepository.hentIkkeTaHensyn().forEach {
                refreshHentAntallOppgaver(oppgaveTjeneste, it)
            }
            statistikkRepository.hentFerdigstilteOgNyeHistorikkMedYtelsetypeSiste8Uker(refresh = true)
        } catch (e: Exception) {
            log.error("", e)
        }
    }
}


@KtorExperimentalAPI
private fun refreshHentAntallOppgaver(
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