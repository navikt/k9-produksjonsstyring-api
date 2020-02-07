package no.nav.helse

import com.github.tomakehurst.wiremock.WireMockServer
import no.nav.helse.dusseldorf.testsupport.jws.ClientCredentials
import no.nav.helse.dusseldorf.testsupport.wiremock.getAzureV1WellKnownUrl
import no.nav.helse.dusseldorf.testsupport.wiremock.getAzureV2WellKnownUrl
import no.nav.helse.dusseldorf.testsupport.wiremock.getNaisStsWellKnownUrl

object TestConfiguration {

    fun asMap(
            wireMockServer: WireMockServer? = null,
            port : Int = 8080,
            dokarkivUrl : String? = wireMockServer?.getDokarkivUrl(),

            azureAuthorizedClients: Set<String> = setOf("azure-client-1", "azure-client-2","azure-client-3"),
            k9ProduksjonsstyringAzureClientId: String = "k9-produkjsonsstyring-api"

    ) : Map<String, String>{
        val map = mutableMapOf(
            Pair("ktor.deployment.port","$port"),
            Pair("nav.dokarkiv_base_url", "$dokarkivUrl")
        )

        // Clients
        wireMockServer?.apply {
            map["nav.auth.clients.0.alias"] = "nais-sts"
            map["nav.auth.clients.0.client_id"] = "srvk9-produkjsonsstyring-api"
            map["nav.auth.clients.0.client_secret"] = "very-secret"
            map["nav.auth.clients.0.discovery_endpoint"] = wireMockServer.getNaisStsWellKnownUrl()
        }

        wireMockServer?.apply {
            map["nav.auth.clients.1.alias"] = "azure-v2"
            map["nav.auth.clients.1.client_id"] = "srvk9-produkjsonsstyring-api"
            map["nav.auth.clients.1.private_key_jwk"] = ClientCredentials.ClientA.privateKeyJwk
            map["nav.auth.clients.1.certificate_hex_thumbprint"] = ClientCredentials.ClientA.certificateHexThumbprint
            map["nav.auth.clients.1.discovery_endpoint"] = wireMockServer.getAzureV2WellKnownUrl()
            map["nav.auth.scopes.hente-dokument"] = "srvpleiepenger-dokument/.default"
        }

        // Issuers
        wireMockServer?.apply {
            map["nav.auth.issuers.0.type"] = "azure"
            map["nav.auth.issuers.0.alias"] = "azure-v1"
            map["nav.auth.issuers.0.discovery_endpoint"] = wireMockServer.getAzureV1WellKnownUrl()
            map["nav.auth.issuers.0.audience"] = k9ProduksjonsstyringAzureClientId
            map["nav.auth.issuers.0.azure.require_certificate_client_authentication"] = "true"
            map["nav.auth.issuers.0.azure.authorized_clients"] = azureAuthorizedClients.joinToString(",")

            map["nav.auth.issuers.1.type"] = "azure"
            map["nav.auth.issuers.1.alias"] = "azure-v2"
            map["nav.auth.issuers.1.discovery_endpoint"] = wireMockServer.getAzureV2WellKnownUrl()
            map["nav.auth.issuers.1.audience"] = k9ProduksjonsstyringAzureClientId
            map["nav.auth.issuers.1.azure.require_certificate_client_authentication"] = "true"
            map["nav.auth.issuers.1.azure.authorized_clients"] = azureAuthorizedClients.joinToString(",")
        }

        return map.toMap()
    }
}