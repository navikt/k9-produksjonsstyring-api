package no.nav.k9.kafka

import no.nav.k9.Configuration
import no.nav.k9.aksjonspunktbehandling.AksjonspunktStream
import no.nav.k9.aksjonspunktbehandling.K9sakEventHandler
import org.slf4j.LoggerFactory

internal class AsynkronProsesseringV1Service(
    kafkaConfig: KafkaConfig,
    configuration: Configuration,
    k9sakEventHandler: K9sakEventHandler
//    gosysOppgaveGateway: GosysOppgaveGateway
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(AsynkronProsesseringV1Service::class.java)
    }


    private val aksjonspunktStream = AksjonspunktStream(
        kafkaConfig = kafkaConfig,
        configuration = configuration,
        k9sakEventHandler1 = k9sakEventHandler
//        gosysOppgaveGateway= gosysOppgaveGateway
    )


    private val sakOgBehandlingStream = AksjonspunktStream(
        kafkaConfig = kafkaConfig,
        configuration = configuration,
        k9sakEventHandler1 = k9sakEventHandler
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