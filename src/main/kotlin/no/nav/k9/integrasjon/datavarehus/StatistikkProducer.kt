package no.nav.k9.integrasjon.datavarehus

import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.dusseldorf.ktor.health.HealthCheck
import no.nav.helse.dusseldorf.ktor.health.Healthy
import no.nav.helse.dusseldorf.ktor.health.Result
import no.nav.helse.dusseldorf.ktor.health.UnHealthy
import no.nav.k9.Configuration
import no.nav.k9.aksjonspunktbehandling.objectMapper
import no.nav.k9.integrasjon.kafka.KafkaConfig
import no.nav.k9.integrasjon.kafka.TopicEntry
import no.nav.k9.integrasjon.kafka.TopicUse
import no.nav.k9.statistikk.kontrakter.Behandling
import no.nav.k9.statistikk.kontrakter.Sak
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.Serializer
import org.apache.kafka.common.serialization.StringSerializer
import org.json.JSONObject
import org.slf4j.LoggerFactory

class StatistikkProducer @KtorExperimentalAPI constructor(
    val kafkaConfig: KafkaConfig,
    val config: Configuration
) : HealthCheck {
    @KtorExperimentalAPI
    private val TOPIC_USE_STATISTIKK_SAK = TopicUse(
        name = config.getStatistikkSakTopic(),
        valueSerializer = Serializer()
    )
    @KtorExperimentalAPI
    private val TOPIC_USE_STATISTIKK_BEHANDLING = TopicUse(
        name = config.getStatistikkBehandlingTopic(),
        valueSerializer = Serializer()
    )
    private companion object {
        private val NAME = "StatistikkProducer"
      
        private val logger = LoggerFactory.getLogger(StatistikkProducer::class.java)
    }

    private val producer: KafkaProducer<String, String> = KafkaProducer(
        kafkaConfig.producer(NAME),
        StringSerializer(),
        StringSerializer()
    )

    @KtorExperimentalAPI
    internal fun sendSak(
        sak: Sak
    ) {
        val melding = objectMapper().writeValueAsString(sak)
        val recordMetaData = producer.send(
           ProducerRecord(
                TOPIC_USE_STATISTIKK_SAK.name,
               melding
            )
        ).get()
        logger.info("Sendt til Topic '${TOPIC_USE_STATISTIKK_SAK.name}' med offset '${recordMetaData.offset()}' til partition '${recordMetaData.partition()}'")
        logger.info("Statistikk sak: $melding")
    }

    @KtorExperimentalAPI
    internal fun sendBehandling(
        behandling: Behandling
    ) {
        val melding = objectMapper().writeValueAsString(behandling)
        val recordMetaData = producer.send(
            ProducerRecord(
                TOPIC_USE_STATISTIKK_BEHANDLING.name,
                melding
            )
        ).get()
        logger.info("Sendt til Topic '${TOPIC_USE_STATISTIKK_BEHANDLING.name}' med offset '${recordMetaData.offset()}' til partition '${recordMetaData.partition()}'")
        logger.info("Statistikk behanlding: $melding")
    }


    internal fun stop() = producer.close()
    @KtorExperimentalAPI
    override suspend fun check(): Result {
        val result = try {
            producer.partitionsFor(TOPIC_USE_STATISTIKK_SAK.name)
            Healthy(NAME, "Tilkobling til Kafka OK!")
        } catch (cause: Throwable) {
            logger.error("Feil ved tilkobling til Kafka", cause)
            UnHealthy(NAME, "Feil ved tilkobling mot Kafka. ${cause.message}")
        }

       try {
            producer.partitionsFor(TOPIC_USE_STATISTIKK_BEHANDLING.name)
            Healthy(NAME, "Tilkobling til Kafka OK!")
        } catch (cause: Throwable) {
            logger.error("Feil ved tilkobling til Kafka", cause)
            return UnHealthy(NAME, "Feil ved tilkobling mot Kafka. ${cause.message}")
        }
        return result
    }

}

private class Serializer : Serializer<TopicEntry<JSONObject>> {
    override fun serialize(topic: String, data: TopicEntry<JSONObject>): ByteArray {
        val metadata = JSONObject()
            .put("correlation_id", data.metadata.correlationId)
            .put("request_id", data.metadata.requestId)
            .put("version", data.metadata.version)

        return JSONObject()
            .put("metadata", metadata)
            .put("data", data.data)
            .toString()
            .toByteArray()
    }

    override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) {}
    override fun close() {}
}

