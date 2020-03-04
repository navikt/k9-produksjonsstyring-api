package no.nav.k9.aksjonspunktbehandling

import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.kafka.ManagedKafkaStreams
import no.nav.helse.kafka.ManagedStreamHealthy
import no.nav.helse.kafka.ManagedStreamReady
import no.nav.k9.AccessTokenClientResolver
import no.nav.k9.Configuration
import no.nav.k9.domene.repository.BehandlingProsessEventRepository
import no.nav.k9.domene.repository.OppgaveRepository
import no.nav.k9.integrasjon.gosys.GosysOppgaveGateway
import no.nav.k9.kafka.KafkaConfig
import no.nav.vedtak.felles.integrasjon.kafka.BehandlingProsessEventDto
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.slf4j.LoggerFactory

internal class AksjonspunktStream(
    kafkaConfig: KafkaConfig,
    oppgaveRepository: OppgaveRepository,
    behandlingProsessEventRepository: BehandlingProsessEventRepository,
    configuration: Configuration
//    gosysOppgaveGateway: GosysOppgaveGateway
) {

    private val stream = ManagedKafkaStreams(
        name = NAME,
        properties = kafkaConfig.stream(NAME),
        topology = topology(
            oppgaveRepository = oppgaveRepository,
            behandlingProsessEventRepository = behandlingProsessEventRepository,
            configuration = configuration
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
            oppgaveRepository: OppgaveRepository,
            behandlingProsessEventRepository: BehandlingProsessEventRepository,
            configuration: Configuration
//            gosysOppgaveGateway: GosysOppgaveGateway
        ): Topology {
            val builder = StreamsBuilder()
            val fromTopic = Topic(
                name = configuration.getAksjonspunkthendelseTopic(),
                serDes = AksjonspunktLaget()
            )
            builder
                .stream<String, TopicEntry<BehandlingProsessEventDto>>(
                    fromTopic.name,
                    Consumed.with(fromTopic.keySerde, fromTopic.valueSerde)
                )
                .foreach { _, topicEntry ->
                    val event = topicEntry.data
                    K9sakEventHandler(
                        oppgaveRepository = oppgaveRepository,
                        behandlingProsessEventRepository = behandlingProsessEventRepository
//                        gosysOppgaveGateway = gosysOppgaveGateway
                    ).prosesser(event)
                }
            return builder.build()
        }
    }

    internal fun stop() = stream.stop(becauseOfError = false)
}