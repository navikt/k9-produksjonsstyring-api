package no.nav.k9.oppgavebehandling

import no.nav.helse.kafka.ManagedKafkaStreams
import no.nav.helse.kafka.ManagedStreamHealthy
import no.nav.helse.kafka.ManagedStreamReady
import no.nav.k9.kafka.KafkaConfig
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Produced
import org.slf4j.LoggerFactory

internal class OpprettOppgaveStream(
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
        private const val NAME = "OpprettOppgaveV1"
        private val logger = LoggerFactory.getLogger("no.nav.$NAME.topology")

        private fun topology() : Topology {
            val builder = StreamsBuilder()
            val fromTopic = Topics.OPPGAVE_OPPRETTET
            val toTopic = Topics.OPPGAVE_OPPRETTET

            builder
                .stream<String, TopicEntry<Any>>(fromTopic.name, Consumed.with(fromTopic.keySerde, fromTopic.valueSerde))

                .to(toTopic.name, Produced.with(toTopic.keySerde, toTopic.valueSerde))
            return builder.build()
        }
    }

    internal fun stop() = stream.stop(becauseOfError = false)
}