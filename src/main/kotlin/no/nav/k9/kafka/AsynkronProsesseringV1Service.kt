package no.nav.k9.kafka

import no.nav.k9.AccessTokenClientResolver
import no.nav.k9.aksjonspunktbehandling.AksjonspunktStream
import no.nav.k9.domene.repository.BehandlingProsessEventRepository
import no.nav.k9.domene.repository.OppgaveRepository
import no.nav.k9.integrasjon.gosys.GosysOppgaveGateway
import org.slf4j.LoggerFactory

internal class AsynkronProsesseringV1Service(
    kafkaConfig: KafkaConfig,
    oppgaveRepository: OppgaveRepository,
    behandlingProsessEventRepository: BehandlingProsessEventRepository
//    gosysOppgaveGateway: GosysOppgaveGateway
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(AsynkronProsesseringV1Service::class.java)
    }


    private val aksjonspunktStream = AksjonspunktStream(
        kafkaConfig = kafkaConfig,
        oppgaveRepository = oppgaveRepository,
        behandlingProsessEventRepository = behandlingProsessEventRepository
//        gosysOppgaveGateway= gosysOppgaveGateway
    )


    private val healthChecks = setOf(
        aksjonspunktStream.healthy
    )

    private val isReadyChecks = setOf(
        aksjonspunktStream.ready
    )

    internal fun stop() {
        logger.info("Stopper streams.")
        aksjonspunktStream.stop()
        logger.info("Alle streams stoppet.")
    }

    internal fun healthChecks() = healthChecks
    internal fun isReadyChecks() = isReadyChecks
}