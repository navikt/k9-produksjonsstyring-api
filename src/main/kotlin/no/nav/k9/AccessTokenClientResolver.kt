package no.nav.k9

import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.dusseldorf.ktor.auth.Client
import no.nav.helse.dusseldorf.ktor.auth.ClientSecretClient
import no.nav.helse.dusseldorf.ktor.auth.PrivateKeyClient
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.DirectKeyId
import no.nav.helse.dusseldorf.oauth2.client.FromJwk
import no.nav.helse.dusseldorf.oauth2.client.SignedJwtAccessTokenClient
import no.nav.k9.Configuration.Companion.AZURE_V2_ALIAS
import no.nav.k9.Configuration.Companion.NAIS_STS_ALIAS
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@KtorExperimentalAPI
class AccessTokenClientResolver(
    private val clients: Map<String, Client>
) {

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(AccessTokenClientResolver::class.java)
    }

    private val naisSts: AccessTokenClient
    private val accessTokenClient: AccessTokenClient
    private val azureClientId: String

    init {
        val naisStsClient = naisStsClient()
        naisSts = NaisStsAccessTokenClient(
            clientId = naisStsClient.clientId(),
            clientSecret = naisStsClient.clientSecret,
            tokenEndpoint = naisStsClient.tokenEndpoint()
        )

        val azureV2Client = azureV2Client()
        accessTokenClient = if (azureV2Client == null) {
            logger.info("Bruker Client[$NAIS_STS_ALIAS] ved kommunikasjon med k9-los-api")
            azureClientId = ""
            naisSts
        } else {
            logger.info("Bruker Client[$AZURE_V2_ALIAS] ved kommunikasjon med k9-los-api")
            azureClientId = azureV2Client.clientId()
            SignedJwtAccessTokenClient(
                clientId = azureV2Client.clientId(),
                tokenEndpoint = azureV2Client.tokenEndpoint(),
                privateKeyProvider = FromJwk(azureV2Client.privateKeyJwk),
                keyIdProvider = DirectKeyId(azureV2Client.certificateHexThumbprint)
            )
        }
    }


    private fun naisStsClient() : ClientSecretClient {
        val client = clients.getOrElse(NAIS_STS_ALIAS) {
            throw IllegalStateException("Client[$NAIS_STS_ALIAS] må være satt opp.")
        }
        return client as ClientSecretClient
    }

    private fun azureV2Client() : PrivateKeyClient? {
        val client = clients.getOrElse(AZURE_V2_ALIAS) {
            return null
        }
        return client as PrivateKeyClient
    }

    internal fun naisSts() = naisSts
    internal fun accessTokenClient() = accessTokenClient
    internal fun azureClientId() = azureClientId
}