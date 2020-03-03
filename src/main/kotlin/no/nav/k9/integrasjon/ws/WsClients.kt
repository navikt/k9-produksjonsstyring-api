package no.nav.k9.integrasjon.ws

import no.nav.tjeneste.virksomhet.sakogbehandling.v1.binding.SakOgBehandlingV1
import org.apache.cxf.feature.LoggingFeature
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.apache.cxf.ws.addressing.WSAddressingFeature

class WsClients {
    fun sakOgBehandling(): SakOgBehandlingV1 =
        createServicePort(
            serviceUrl = "sakOgBehandlingUrl",
            serviceClazz = SakOgBehandlingV1::class.java
        )

    private fun <PORT_TYPE : Any> createServicePort(
        serviceUrl: String,
        serviceClazz: Class<PORT_TYPE>
    ): PORT_TYPE = JaxWsProxyFactoryBean()
        .apply {
            address = serviceUrl
            serviceClass = serviceClazz
            features = listOf(WSAddressingFeature(), LoggingFeature())
        }
        .create(serviceClazz)
        .apply {
//            when (environment.application.profile) {
//                Profile.LOCAL_TEST, Profile.LOCAL -> {
//                    stsClient.configureFor(this, STS_SAML_POLICY_NO_TRANSPORT_BINDING)
//                }
//                else -> {
//                    if (withOidcContext) {
//                        stsClient.configureRequestSamlTokenOnBehalfOfOidc(this)
//                    } else {
//                        stsClient.configureFor(this, STS_SAML_POLICY)
//                    }
//                }
//            }
        }
}