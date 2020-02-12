package no.nav.k9.aksjonspunktbehandling

import no.nav.helse.kafka.ManagedKafkaStreams
import no.nav.helse.kafka.ManagedStreamHealthy
import no.nav.helse.kafka.ManagedStreamReady
import no.nav.k9.kafka.KafkaConfig
import no.nav.vedtak.felles.integrasjon.kafka.BehandlingProsessEventDto
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.slf4j.LoggerFactory

internal class AksjonspunktStream(
    kafkaConfig: KafkaConfig
) {

    private val stream = ManagedKafkaStreams(
        name = NAME,
        properties = kafkaConfig.stream(NAME),
        topology = topology(),
        unreadyAfterStreamStoppedIn = kafkaConfig.unreadyAfterStreamStoppedIn
    )

    internal val ready = ManagedStreamReady(stream)
    internal val healthy = ManagedStreamHealthy(stream)

    private companion object {
        private const val NAME = "AksjonspunktLagetV1"
        private val logger = LoggerFactory.getLogger("no.nav.$NAME.topology")

        private fun topology() : Topology {
            val builder = StreamsBuilder()
            val fromTopic = Topics.AKSJONSPUNKT_LAGET

            builder
                .stream<String, TopicEntry<BehandlingProsessEventDto>>(fromTopic.name, Consumed.with(fromTopic.keySerde, fromTopic.valueSerde))
                .foreach { _, topicEntry ->
                    val event = topicEntry.data
                    K9sakEventHandler().prosesser(event)
                }
            return builder.build()
        }
    }

    internal fun stop() = stream.stop(becauseOfError = false)
}