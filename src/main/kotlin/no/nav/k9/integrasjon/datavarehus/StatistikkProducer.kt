package no.nav.k9.integrasjon.datavarehus

import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.runBlocking
import no.nav.helse.dusseldorf.ktor.health.HealthCheck
import no.nav.helse.dusseldorf.ktor.health.Healthy
import no.nav.helse.dusseldorf.ktor.health.Result
import no.nav.helse.dusseldorf.ktor.health.UnHealthy
import no.nav.k9.Configuration
import no.nav.k9.KoinProfile
import no.nav.k9.aksjonspunktbehandling.objectMapper
import no.nav.k9.domene.modell.Modell
import no.nav.k9.domene.repository.ReservasjonRepository
import no.nav.k9.domene.repository.SaksbehandlerRepository
import no.nav.k9.integrasjon.abac.IPepClient
import no.nav.k9.integrasjon.abac.PepClient
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
    val saksbehandlerRepository: SaksbehandlerRepository,
    val reservasjonRepository: ReservasjonRepository,
    val pepClient: IPepClient,
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

        private val log = LoggerFactory.getLogger(StatistikkProducer::class.java)
    }

    private val producer: KafkaProducer<String, String> = KafkaProducer(
        kafkaConfig.producer(NAME),
        StringSerializer(),
        StringSerializer()
    )

    @KtorExperimentalAPI
    fun send(modell: Modell) {
        if (config.koinProfile() == KoinProfile.LOCAL) {
            return
        }
        runBlocking {
            if (pepClient.kanSendeSakTilStatistikk(modell.sisteEvent().saksnummer)) {
                sendSak(modell.dvhSak())
                sendBehandling(
                    modell.dvhBehandling(
                        saksbehandlerRepository = saksbehandlerRepository,
                        reservasjonRepository = reservasjonRepository
                    )
                )
            }
        }
    }

    @KtorExperimentalAPI
    private fun sendSak(
        sak: Sak
    ) {
        if (config.koinProfile() == KoinProfile.LOCAL) {
            log.info("Lokal kjøring, sender ikke melding til statistikk")
            return
        }
        val melding = objectMapper().writeValueAsString(sak)
        val recordMetaData = producer.send(
            ProducerRecord(
                TOPIC_USE_STATISTIKK_SAK.name,
                melding
            )
        ).get()
      //  log.info("Sendt til Topic '${TOPIC_USE_STATISTIKK_SAK.name}' med offset '${recordMetaData.offset()}' til partition '${recordMetaData.partition()}'")
      //  log.info("Statistikk sak: $melding")
    }

    @KtorExperimentalAPI
    private fun sendBehandling(
        behandling: Behandling
    ) {
        if (config.koinProfile() == KoinProfile.LOCAL) {
            log.info("Lokal kjøring, sender ikke melding til statistikk")
            return
        }
       
        val melding = objectMapper().writeValueAsString(behandling)
        val recordMetaData = producer.send(
            ProducerRecord(
                TOPIC_USE_STATISTIKK_BEHANDLING.name,
                melding
            )
        ).get()
//        log.info("Sendt til Topic '${TOPIC_USE_STATISTIKK_BEHANDLING.name}' med offset '${recordMetaData.offset()}' til partition '${recordMetaData.partition()}'")
//        log.info("Statistikk behandling: $melding")
    }


    internal fun stop() = producer.close()

    @KtorExperimentalAPI
    override suspend fun check(): Result {
        val result = try {
            producer.partitionsFor(TOPIC_USE_STATISTIKK_SAK.name)
            Healthy(NAME, "Tilkobling til Kafka OK!")
        } catch (cause: Throwable) {
            log.error("Feil ved tilkobling til Kafka", cause)
            UnHealthy(NAME, "Feil ved tilkobling mot Kafka. ${cause.message}")
        }

        try {
            producer.partitionsFor(TOPIC_USE_STATISTIKK_BEHANDLING.name)
            Healthy(NAME, "Tilkobling til Kafka OK!")
        } catch (cause: Throwable) {
            log.error("Feil ved tilkobling til Kafka", cause)
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

