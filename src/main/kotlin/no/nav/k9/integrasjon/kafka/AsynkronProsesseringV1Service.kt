package no.nav.k9.integrasjon.kafka

import io.ktor.util.KtorExperimentalAPI
import no.nav.k9.Configuration
import no.nav.k9.aksjonspunktbehandling.AksjonspunktStream
import no.nav.k9.aksjonspunktbehandling.K9sakEventHandler
import org.slf4j.LoggerFactory

internal class AsynkronProsesseringV1Service @KtorExperimentalAPI constructor(
    kafkaConfig: KafkaConfig,
    configuration: Configuration,
    k9sakEventHandler: K9sakEventHandler
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(AsynkronProsesseringV1Service::class.java)
    }


    @KtorExperimentalAPI
    private val aksjonspunktStream = AksjonspunktStream(
        kafkaConfig = kafkaConfig,
        configuration = configuration,
        k9sakEventHandler = k9sakEventHandler
    )



    @KtorExperimentalAPI
    private val healthChecks = setOf(
        aksjonspunktStream.healthy
    )

    @KtorExperimentalAPI
    private val isReadyChecks = setOf(
        aksjonspunktStream.ready
    )

    @KtorExperimentalAPI
    internal fun stop() {
        logger.info("Stopper streams.")
        aksjonspunktStream.stop()
        logger.info("Alle streams stoppet.")
    }

    @KtorExperimentalAPI
    internal fun healthChecks() = healthChecks
    @KtorExperimentalAPI
    internal fun isReadyChecks() = isReadyChecks
}