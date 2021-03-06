package no.nav.k9

import com.github.kittinunf.fuel.httpGet
import com.github.tomakehurst.wiremock.WireMockServer
import no.nav.common.KafkaEnvironment
import no.nav.helse.dusseldorf.testsupport.jws.ClientCredentials
import no.nav.k9.wiremocks.getTpsProxyUrl
import org.json.JSONObject

object TestConfiguration {

    fun asMap(
        wireMockServer: WireMockServer? = null,
        kafkaEnvironment: KafkaEnvironment? = null,
        port: Int = 8020,
        tpsProxyBaseUrl: String? = wireMockServer?.getTpsProxyUrl()
    ): Map<String, String> {
        val map = mutableMapOf(
            Pair("ktor.deployment.port", "$port"),
            Pair("nav.register_urls.tps_proxy_v1", "$tpsProxyBaseUrl"),
            Pair("nav.register_urls.pdl_url", "$tpsProxyBaseUrl")
        )

        map["nav.audit.enabled"] = "false"
        map["nav.audit.vendor"] = "test"
        map["nav.audit.product"] = "test"
        map["nav.auth.trustore.path"] = "vtp"
        map["nav.auth.trustore.password"] = "vtp"
        map["nav.abac.system_user"] = "very"
        map["nav.abac.system_user_password"] = "secret"
        map["nav.abac.url"] = "https://url"

        map["nav.auth.clients.0.alias"] = "nais-sts"
        map["nav.auth.clients.0.client_id"] = "srvpps-k9-los-api"
        map["nav.auth.clients.0.client_secret"] = "very-secret"
        map["nav.auth.clients.0.discovery_endpoint"] =
            "https://vtp:8063/rest/isso/oauth2/.well-known/openid-configuration"

        map["nav.auth.clients.1.alias"] = "azure-v2"
        map["nav.auth.clients.1.client_id"] = "pleiepengesoknad-prosessering"
        map["nav.auth.clients.1.private_key_jwk"] = ClientCredentials.ClientA.privateKeyJwk
        map["nav.auth.clients.1.certificate_hex_thumbprint"] = ClientCredentials.ClientA.certificateHexThumbprint
        map["nav.auth.clients.1.discovery_endpoint"] = "http://azure-mock:8100/v2.0/.well-known/openid-configuration"

        map["nav.kafka.bootstrap_servers"] = kafkaEnvironment?.brokersURL ?: "vtp:9092"
        map["nav.kafka.username"] = kafkaEnvironment?.username() ?: "vtp"
        map["nav.kafka.password"] = kafkaEnvironment?.password() ?: "vtp"
        map["nav.kafka.unready_after_stream_stopped_in.amount"] = "1010"
        map["nav.kafka.unready_after_stream_stopped_in.unit"] = "SECONDS"
        map["nav.trust_store.path"] = "${System.getProperty("user.home")}/.modig/truststore.jks"
        map["nav.trust_store.password"] = "changeit"

        map["nav.db.url"] = "jdbc:postgresql://localhost:5432/k9los_unit"
        map["nav.db.username"] = "k9los_unit"
        map["nav.db.password"] = "k9los_unit"
        return map.toMap()
    }

    private fun String.getAsJson() = JSONObject(this.httpGet().responseString().third.component1())
}
