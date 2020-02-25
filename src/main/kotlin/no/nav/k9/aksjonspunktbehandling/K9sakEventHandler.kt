package no.nav.k9.aksjonspunktbehandling

import io.ktor.util.KtorExperimentalAPI
import no.nav.k9.AccessTokenClientResolver
import no.nav.k9.domene.repository.BehandlingProsessEventRepository
import no.nav.k9.domene.repository.OppgaveRepository
import no.nav.k9.integrasjon.gosys.GosysKonstanter
import no.nav.k9.integrasjon.gosys.GosysOppgaveGateway
import no.nav.k9.integrasjon.gosys.HentGosysOppgaverRequest
import no.nav.k9.integrasjon.gosys.OpprettGosysOppgaveRequest
import no.nav.vedtak.felles.integrasjon.kafka.BehandlingProsessEventDto
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*

class K9sakEventHandler @KtorExperimentalAPI constructor(
    val oppgaveRepository: OppgaveRepository,
    val behandlingProsessEventRepository: BehandlingProsessEventRepository,
    val gosysOppgaveGateway: GosysOppgaveGateway
) {
    private val log = LoggerFactory.getLogger(K9sakEventHandler::class.java)

    @KtorExperimentalAPI
    fun prosesser(event: BehandlingProsessEventDto) {
        val modell = behandlingProsessEventRepository.lagreBehandlingProsessEvent(event)
        val oppgave =   modell.syncOppgaveTilGosys(gosysOppgaveGateway)
        oppgaveRepository.opprettOppgave(oppgave)
    }
}