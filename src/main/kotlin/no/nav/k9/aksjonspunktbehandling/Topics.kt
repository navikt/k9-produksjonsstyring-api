package no.nav.k9.aksjonspunktbehandling

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.dusseldorf.ktor.jackson.dusseldorfConfigured
import no.nav.k9.kafka.dto.BehandlingProsessEventDto
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.serialization.Serializer
import org.apache.kafka.common.serialization.StringSerializer


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
        name = "privat-k9-aksjonspunkthendelse",
        serDes = AksjonspunktLaget()
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

internal class AksjonspunktLaget : SerDes<BehandlingProsessEventDto>() {
    override fun deserialize(topic: String?, data: ByteArray?): BehandlingProsessEventDto? {
        return data?.let {
            //objectMapper.readValue(it)
            null
        }
    }
}