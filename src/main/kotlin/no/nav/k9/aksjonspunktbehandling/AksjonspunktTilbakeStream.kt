package no.nav.k9.aksjonspunktbehandling

import io.ktor.util.*
import no.nav.helse.kafka.ManagedKafkaStreams
import no.nav.helse.kafka.ManagedStreamHealthy
import no.nav.helse.kafka.ManagedStreamReady
import no.nav.k9.Configuration
import no.nav.k9.integrasjon.kafka.KafkaConfig
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.slf4j.LoggerFactory

internal class AksjonspunktTilbakeStream @KtorExperimentalAPI constructor(
    kafkaConfig: KafkaConfig,
    configuration: Configuration,
    k9TilbakeEventHandler: K9TilbakeEventHandler
) {

    @KtorExperimentalAPI
    private val stream = ManagedKafkaStreams(
        name = NAME,
        properties = kafkaConfig.stream(NAME),
        topology = topology(
            configuration = configuration,
            k9TilbakeEventHandler = k9TilbakeEventHandler
        ),
        unreadyAfterStreamStoppedIn = kafkaConfig.unreadyAfterStreamStoppedIn
    )

    @KtorExperimentalAPI
    internal val ready = ManagedStreamReady(stream)
    @KtorExperimentalAPI
    internal val healthy = ManagedStreamHealthy(stream)

    private companion object {
        private const val NAME = "TilbakeV1"
        private val log = LoggerFactory.getLogger("no.nav.$NAME.topology")

        @KtorExperimentalAPI
        private fun topology(
            configuration: Configuration,
            k9TilbakeEventHandler: K9TilbakeEventHandler
        ): Topology {
            val builder = StreamsBuilder()
            val fromTopic = Topic(
                name = configuration.getAksjonspunkthendelseTilbakeTopic(),
                serDes = AksjonspunktLagetTilbake()
            )
            builder
                .stream(
                    fromTopic.name,
                    Consumed.with(fromTopic.keySerde, fromTopic.valueSerde)
                )
                .foreach { _, entry ->
                    if (entry != null) {
                        log.info("Prosesserer entry fra tilbakekreving")
                        k9TilbakeEventHandler.prosesser(entry)
                    }
                }
            return builder.build()
        }
    }

    @KtorExperimentalAPI
    internal fun stop() = stream.stop(becauseOfError = false)
}