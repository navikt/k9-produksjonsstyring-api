package no.nav.k9.aksjonspunktbehandling

import io.ktor.util.KtorExperimentalAPI
import no.nav.k9.domene.repository.BehandlingProsessEventRepository
import no.nav.k9.domene.repository.OppgaveRepository
import no.nav.k9.integrasjon.gosys.GosysOppgaveGateway
import no.nav.k9.integrasjon.sakogbehandling.sendBehandlingAvsluttet
import no.nav.k9.integrasjon.sakogbehandling.sendBehandlingOpprettet
import no.nav.melding.virksomhet.behandlingsstatus.hendelsehandterer.v1.hendelseshandtererbehandlingsstatus.BehandlingAvsluttet
import no.nav.melding.virksomhet.behandlingsstatus.hendelsehandterer.v1.hendelseshandtererbehandlingsstatus.BehandlingOpprettet
import no.nav.vedtak.felles.integrasjon.kafka.BehandlingProsessEventDto
import org.slf4j.LoggerFactory

class K9sakEventHandler @KtorExperimentalAPI constructor(
    val oppgaveRepository: OppgaveRepository,
    val behandlingProsessEventRepository: BehandlingProsessEventRepository,
    val gosysOppgaveGateway: GosysOppgaveGateway
) {
    private val log = LoggerFactory.getLogger(K9sakEventHandler::class.java)

    @KtorExperimentalAPI
    fun prosesser(event: BehandlingProsessEventDto) {
        val modell = behandlingProsessEventRepository.lagre(event)


        // Sjekk om behandlingen starter eller avsluttes, skal da sende en melding til behandlesak for å fortelle modia.
        if (false) {
            sendBehandlingOpprettet(BehandlingOpprettet())
            sendBehandlingAvsluttet(BehandlingAvsluttet())
        }


        val oppgave = modell.syncOppgaveTilGosys(gosysOppgaveGateway)
        oppgaveRepository.lagre(oppgave)
    }
}