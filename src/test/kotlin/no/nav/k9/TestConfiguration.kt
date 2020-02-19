package no.nav.k9

import com.github.kittinunf.fuel.httpGet
import com.github.tomakehurst.wiremock.WireMockServer
import no.nav.common.KafkaEnvironment
import no.nav.helse.dusseldorf.testsupport.jws.ClientCredentials
import no.nav.helse.dusseldorf.testsupport.wiremock.getAzureV2WellKnownUrl
import no.nav.helse.dusseldorf.testsupport.wiremock.getNaisStsWellKnownUrl
import org.json.JSONObject

object TestConfiguration {

    fun asMap(
        wireMockServer: WireMockServer? = null,
        kafkaEnvironment: KafkaEnvironment? = null,
        port: Int = 8020
    ): Map<String, String> {
        val map = mutableMapOf(
            Pair("ktor.deployment.port", "$port")
        )

        map["nav.auth.trustore.path"] = "vtp"
        map["nav.auth.trustore.password"] = "vtp"

        map["nav.auth.clients.0.alias"] = "nais-sts"
        map["nav.auth.clients.0.client_id"] = "srvpps-k9-los-api"
        map["nav.auth.clients.0.client_secret"] = "very-secret"
        map["nav.auth.clients.0.discovery_endpoint"] = "https://vtp:8063/rest/isso/oauth2/.well-known/openid-configuration"

        map["nav.auth.clients.1.alias"] = "azure-v2"
        map["nav.auth.clients.1.client_id"] = "pleiepengesoknad-prosessering"
        map["nav.auth.clients.1.private_key_jwk"] = ClientCredentials.ClientA.privateKeyJwk
        map["nav.auth.clients.1.certificate_hex_thumbprint"] = ClientCredentials.ClientA.certificateHexThumbprint
        map["nav.auth.clients.1.discovery_endpoint"] = "http://azure-mock:8100/v2.0/.well-known/openid-configuration"

        kafkaEnvironment?.let {
            map["nav.kafka.bootstrap_servers"] = it.brokersURL
            map["nav.kafka.username"] = it.username()
            map["nav.kafka.password"] = it.password()
            map["nav.kafka.unready_after_stream_stopped_in.amount"] = "1010"
            map["nav.kafka.unready_after_stream_stopped_in.unit"] = "SECONDS"
        }
        map["nav.db.url"] = "jdbc:postgresql://localhost:5432/k9los"
        map["nav.db.username"] = "k9los"
        map["nav.db.password"] = "k9los"
        return map.toMap()
    }

    private fun String.getAsJson() = JSONObject(this.httpGet().responseString().third.component1())
}