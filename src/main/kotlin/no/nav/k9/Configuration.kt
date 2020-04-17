package no.nav.k9

import io.ktor.config.ApplicationConfig
import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.dusseldorf.ktor.auth.clients
import no.nav.helse.dusseldorf.ktor.auth.issuers
import no.nav.helse.dusseldorf.ktor.auth.withoutAdditionalClaimRules
import no.nav.helse.dusseldorf.ktor.core.getOptionalString
import no.nav.helse.dusseldorf.ktor.core.getRequiredString
import no.nav.k9.db.createHikariConfig
import no.nav.k9.kafka.KafkaConfig
import no.nav.k9.tilgangskontroll.abac.AbacClientConfig
import java.net.URI
import java.time.Duration
import java.time.temporal.ChronoUnit

@KtorExperimentalAPI
data class Configuration(private val config: ApplicationConfig) {
    companion object {
        internal const val NAIS_STS_ALIAS = "nais-sts"
        internal const val AZURE_V2_ALIAS = "azure-v2"
    }

    private val clients = config.clients()

    internal fun issuers() = config.issuers().withoutAdditionalClaimRules()

    internal fun clients() = clients

    internal fun pdlUrl() = URI(config.getRequiredString("nav.register_urls.pdl_url", secret = false))

    // private fun azureClientConfigured() = clients().containsKey(AZURE_V2_ALIAS)

    internal fun abacClient() = AbacClientConfig(
        username = config.getRequiredString("nav.abac.system_user", secret = false),
        password = config.getRequiredString("nav.abac.system_user_password", secret = true),
        endpointUrl = config.getRequiredString("nav.abac.pdp_url", secret = false)
    )

    internal fun hikariConfig() = createHikariConfig(
        jdbcUrl = config.getRequiredString("nav.db.url", secret = false),
        username = config.getOptionalString("nav.db.username", secret = false),
        password = config.getOptionalString("nav.db.password", secret = true)
    )

    internal fun getAksjonspunkthendelseTopic(): String {
        return config.getOptionalString("nav.kafka.aksjonshendelseTopic", secret = false)
            ?: "privat-k9-aksjonspunkthendelse"
    }

    internal fun getKafkaConfig() =
        config.getRequiredString("nav.kafka.bootstrap_servers", secret = false).let { bootstrapServers ->
            val trustStore = config.getRequiredString("nav.trust_store.path", secret = false).let { trustStorePath ->
                config.getOptionalString("nav.trust_store.password", secret = true)?.let { trustStorePassword ->
                    Pair(trustStorePath, trustStorePassword)
                }
            }

            KafkaConfig(
                bootstrapServers = bootstrapServers,
                credentials = Pair(
                    config.getRequiredString("nav.kafka.username", secret = false),
                    config.getRequiredString("nav.kafka.password", secret = true)
                ),
                trustStore = trustStore,
                exactlyOnce = false, // settes til true når vi skal gå mot prod cluster
                unreadyAfterStreamStoppedIn = unreadyAfterStreamStoppedIn()
            )
        }

    internal fun getCookieName(): String {
        return config.getRequiredString("nav.auth.cookie_name", secret = false)
    }

    private fun unreadyAfterStreamStoppedIn() = Duration.of(
        config.getRequiredString("nav.kafka.unready_after_stream_stopped_in.amount", secret = false).toLong(),
        ChronoUnit.valueOf(config.getRequiredString("nav.kafka.unready_after_stream_stopped_in.unit", secret = false))
    )

    fun getOppgaveBaseUri(): URI {
        return URI(config.getRequiredString("nav.gosys.baseuri", secret = false))
    }

    fun erIkkeLokalt(): Boolean {
        return !config.getOptionalString("nav.db.vault_mountpath", secret = false).isNullOrBlank()
    }

    fun erIDevFss(): Boolean {
        val optionalString = config.getOptionalString("nav.clustername", secret = false)
        if (optionalString.isNullOrBlank()) {
            return false
        } else if (optionalString == "dev-fss") {
            return true
        }
        return false
    }

    fun getVaultDbPath(): String {
        return config.getOptionalString("nav.db.vault_mountpath", secret = false)!!
    }

    fun getSakOgBehandlingMqGateway(): String {
        return config.getOptionalString("nav.sak_og_behandling.gateway", secret = false)!!
    }

    fun getSakOgBehandlingMqGatewayHostname(): String {
        return config.getOptionalString("nav.sak_og_behandling.hostname", secret = false)!!
    }

    fun getSakOgBehandlingMqGatewayPort(): String {
        return config.getOptionalString("nav.sak_og_behandling.port", secret = false)!!
    }

    fun databaseName(): String {
        return "k9-los"
    }
}