package no.nav.k9.saogbehandling

import no.nav.helse.dusseldorf.ktor.health.HealthCheck
import no.nav.helse.dusseldorf.ktor.health.Healthy
import no.nav.helse.dusseldorf.ktor.health.Result
import no.nav.helse.dusseldorf.ktor.health.UnHealthy
import no.nav.k9.kafka.*
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.Serializer
import org.json.JSONObject
import org.slf4j.LoggerFactory

class SakOgBehadlingProducer(
    val kafkaConfig: KafkaConfig
) : HealthCheck {

    private companion object {
        private val NAME = "SakOgBehadlingProducer"
        private val TOPIC_USE_SAK_OG_BEHANDLING = TopicUse(
            name = Topics.SAK_OG_BEHANDLING,
            valueSerializer = SakOgBehandlingSerialier()
        )
        private val logger = LoggerFactory.getLogger(SakOgBehadlingProducer::class.java)
    }

    private val producer = KafkaProducer<String, TopicEntry<JSONObject>>(
        kafkaConfig.producer(NAME),
        TOPIC_USE_SAK_OG_BEHANDLING.keySerializer(),
        TOPIC_USE_SAK_OG_BEHANDLING.valueSerializer
    )

    internal fun produce(
        melding: Any,
        metadata: Metadata
    ) {

        val recordMetaData = producer.send(
            ProducerRecord(
                TOPIC_USE_SAK_OG_BEHANDLING.name,
                "key",
                TopicEntry(
                    metadata = metadata,
                    data = JSONObject()
                )
            )
        ).get()
        logger.info("Søknad sendt til Topic '${TOPIC_USE_SAK_OG_BEHANDLING.name}' med offset '${recordMetaData.offset()}' til partition '${recordMetaData.partition()}'")
    }


    internal fun stop() = producer.close()
    override suspend fun check(): Result {
        return try {
            producer.partitionsFor(TOPIC_USE_SAK_OG_BEHANDLING.name)
            Healthy(NAME, "Tilkobling til Kafka OK!")
        } catch (cause: Throwable) {
            logger.error("Feil ved tilkobling til Kafka", cause)
            UnHealthy(NAME, "Feil ved tilkobling mot Kafka. ${cause.message}")
        }
    }

}

private class SakOgBehandlingSerialier : Serializer<TopicEntry<JSONObject>> {
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

