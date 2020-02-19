package no.nav.k9.aksjonspunktbehandling

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.dusseldorf.ktor.jackson.dusseldorfConfigured
import no.nav.vedtak.felles.integrasjon.kafka.BehandlingProsessEventDto
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.serialization.Serializer
import org.apache.kafka.common.serialization.StringSerializer

data class TopicEntry<V>(val metadata: Metadata, val data: V)

internal data class Topic<V>(
    val name: String,
    val serDes : SerDes<V>
) {
    val keySerializer = StringSerializer()
    val keySerde = Serdes.String()
    val valueSerde = Serdes.serdeFrom(serDes, serDes)
}

internal object Topics {
    val AKSJONSPUNKT_LAGET = Topic(
        name = "privat-foreldrepenger-aksjonspunkthendelse-local",
        serDes = AksjosnpunktLaget()
    )
}
fun objectMapper(): ObjectMapper {
    return jacksonObjectMapper()
        .dusseldorfConfigured()
        .configure(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false)
        .setPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CAMEL_CASE)
}

internal abstract class SerDes<V> : Serializer<V>, Deserializer<V> {
    protected val objectMapper = objectMapper()



    override fun serialize(topic: String?, data: V): ByteArray? {
        return data?.let {
            objectMapper.writeValueAsBytes(it)
        }
    }
    override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) {}
    override fun close() {}
}

private class AksjosnpunktLaget: SerDes<TopicEntry<BehandlingProsessEventDto>>() {
    override fun deserialize(topic: String?, data: ByteArray?): TopicEntry<BehandlingProsessEventDto>? {
        return data?.let {
            objectMapper.readValue(it)
        }
    }
}