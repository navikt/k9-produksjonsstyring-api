package no.nav.k9.aksjonspunktbehandling

import io.ktor.util.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.sendBlocking
import no.nav.k9.Configuration
import no.nav.k9.domene.lager.oppgave.Oppgave
import no.nav.k9.domene.repository.*
import no.nav.k9.integrasjon.datavarehus.StatistikkProducer
import no.nav.k9.integrasjon.kafka.dto.PunsjEventDto
import no.nav.k9.integrasjon.sakogbehandling.SakOgBehandlingProducer
import org.slf4j.LoggerFactory


class K9punsjEventHandler @KtorExperimentalAPI constructor(
    val oppgaveRepository: OppgaveRepository,
    val punsjEventK9Repository: PunsjEventK9Repository,
    val oppgaverSomSkalInnPåKøer: Channel<Oppgave>,
    val statistikkRepository: StatistikkRepository
) {
    private val log = LoggerFactory.getLogger(K9punsjEventHandler::class.java)

    @KtorExperimentalAPI
    fun prosesser(
        event: PunsjEventDto
    ) {
        val modell = punsjEventK9Repository.lagre(event = event)
        val oppgave = modell.oppgave()
        oppgaveRepository.lagre(oppgave.eksternId){oppgave}
        oppgaverSomSkalInnPåKøer.sendBlocking(oppgave)
    }

}
