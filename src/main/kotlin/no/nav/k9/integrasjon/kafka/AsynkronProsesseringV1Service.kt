package no.nav.k9.integrasjon.kafka

import io.ktor.util.KtorExperimentalAPI
import no.nav.k9.Configuration
import no.nav.k9.aksjonspunktbehandling.AksjonspunktStreamK9
import no.nav.k9.aksjonspunktbehandling.AksjonspunktTilbakeStream
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
    private val aksjonspunktStream = AksjonspunktStreamK9(
        kafkaConfig = kafkaConfig,
        configuration = configuration,
        k9sakEventHandler = k9sakEventHandler
    )

  @KtorExperimentalAPI
    private val aksjonspunkTilbaketStream = AksjonspunktTilbakeStream(
        kafkaConfig = kafkaConfig,
        configuration = configuration,
        k9sakEventHandler = k9sakEventHandler
    )



    @KtorExperimentalAPI
    private val healthChecks = setOf(
        aksjonspunktStream.healthy,
        aksjonspunkTilbaketStream.healthy
    )

    @KtorExperimentalAPI
    private val isReadyChecks = setOf(
        aksjonspunktStream.ready,
        aksjonspunkTilbaketStream.ready
    )

    @KtorExperimentalAPI
    internal fun stop() {
        logger.info("Stopper streams.")
        aksjonspunktStream.stop()
        aksjonspunkTilbaketStream.stop()
        logger.info("Alle streams stoppet.")
    }

    @KtorExperimentalAPI
    internal fun healthChecks() = healthChecks
    @KtorExperimentalAPI
    internal fun isReadyChecks() = isReadyChecks
}