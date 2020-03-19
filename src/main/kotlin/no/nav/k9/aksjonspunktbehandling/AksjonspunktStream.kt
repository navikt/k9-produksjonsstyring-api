package no.nav.k9.aksjonspunktbehandling

import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.kafka.ManagedKafkaStreams
import no.nav.helse.kafka.ManagedStreamHealthy
import no.nav.helse.kafka.ManagedStreamReady
import no.nav.k9.Configuration
import no.nav.k9.domene.repository.BehandlingProsessEventRepository
import no.nav.k9.domene.repository.OppgaveRepository
import no.nav.k9.kafka.KafkaConfig
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.slf4j.LoggerFactory

internal class AksjonspunktStream(
    kafkaConfig: KafkaConfig,
    configuration: Configuration,
    k9sakEventHandler1: K9sakEventHandler
//    gosysOppgaveGateway: GosysOppgaveGateway
) {

    private val stream = ManagedKafkaStreams(
        name = NAME,
        properties = kafkaConfig.stream(NAME),
        topology = topology(
            configuration = configuration,
            k9sakEventHandler = k9sakEventHandler1
//            gosysOppgaveGateway = gosysOppgaveGateway
        ),
        unreadyAfterStreamStoppedIn = kafkaConfig.unreadyAfterStreamStoppedIn
    )

    internal val ready = ManagedStreamReady(stream)
    internal val healthy = ManagedStreamHealthy(stream)

    private companion object {
        private const val NAME = "AksjonspunktLagetV1"
        private val log = LoggerFactory.getLogger("no.nav.$NAME.topology")

        @KtorExperimentalAPI
        private fun topology(
            configuration: Configuration,
            k9sakEventHandler: K9sakEventHandler
//            gosysOppgaveGateway: GosysOppgaveGateway
        ): Topology {
            val builder = StreamsBuilder()
            val fromTopic = Topic(
                name = configuration.getAksjonspunkthendelseTopic(),
                serDes = AksjonspunktLaget()
            )
            builder
                .stream(
                    fromTopic.name,
                    Consumed.with(fromTopic.keySerde, fromTopic.valueSerde)
                )
                .foreach { _, entry ->
                    val event = entry
                    k9sakEventHandler.prosesser(event)
                }
            return builder.build()
        }
    }

    internal fun stop() = stream.stop(becauseOfError = false)
}