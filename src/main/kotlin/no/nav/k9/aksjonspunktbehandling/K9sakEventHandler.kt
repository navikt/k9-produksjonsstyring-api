package no.nav.k9.aksjonspunktbehandling

import no.nav.k9.aksjonspunktbehandling.eventresultat.EventResultat.*
import no.nav.k9.domene.repository.BehandlingProsessEventRepository
import no.nav.k9.domene.repository.OppgaveRepository
import no.nav.k9.domene.repository.aktiveAksjonspunkt
import no.nav.vedtak.felles.integrasjon.kafka.BehandlingProsessEventDto
import org.slf4j.LoggerFactory

class K9sakEventHandler(
    val oppgaveRepository: OppgaveRepository,
    val behandlingProsessEventRepository: BehandlingProsessEventRepository
) {
    private val log = LoggerFactory.getLogger(K9sakEventHandler::class.java)

    fun prosesser(event: BehandlingProsessEventDto) {
        val modell = behandlingProsessEventRepository.lagreBehandlingProsessEvent(event)
        oppgaveRepository.opprettOppgave(modell.oppgave())
    }
}