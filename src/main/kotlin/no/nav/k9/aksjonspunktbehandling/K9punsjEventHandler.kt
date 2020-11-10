package no.nav.k9.aksjonspunktbehandling

import io.ktor.util.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import no.nav.k9.domene.lager.oppgave.Oppgave
import no.nav.k9.domene.repository.*
import no.nav.k9.integrasjon.kafka.dto.PunsjEventDto
import org.slf4j.LoggerFactory


class K9punsjEventHandler @KtorExperimentalAPI constructor(
    val oppgaveRepository: OppgaveRepository,
    val punsjEventK9Repository: PunsjEventK9Repository,
    val statistikkRepository: StatistikkRepository,
    val saksbehhandlerRepository: SaksbehandlerRepository,
    val statistikkChannel: Channel<Boolean>,
    val reservasjonRepository: ReservasjonRepository,
    val oppgaveKøRepository: OppgaveKøRepository
) {
    private val log = LoggerFactory.getLogger(K9punsjEventHandler::class.java)

    @KtorExperimentalAPI
    fun prosesser(
        event: PunsjEventDto
    ) {
        val modell = punsjEventK9Repository.lagre(event = event)
        val oppgave = modell.oppgave()
        oppgaveRepository.lagre(oppgave.eksternId){oppgave}
        runBlocking {
            for (oppgavekø in oppgaveKøRepository.hentKøIdIkkeTaHensyn()) {
                oppgaveKøRepository.leggTilOppgaverTilKø(oppgavekø, listOf(oppgave), reservasjonRepository)
            }
            statistikkChannel.send(true)
        }
    }

}
