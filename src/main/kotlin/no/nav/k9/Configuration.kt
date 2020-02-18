package no.nav.k9

import io.ktor.config.ApplicationConfig
import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.dusseldorf.ktor.auth.clients
import no.nav.helse.dusseldorf.ktor.auth.issuers
import no.nav.helse.dusseldorf.ktor.auth.withoutAdditionalClaimRules
import no.nav.helse.dusseldorf.ktor.core.getOptionalString
import no.nav.helse.dusseldorf.ktor.core.getRequiredString
import no.nav.k9.db.createHikariConfig
import no.nav.k9.db.hikariConfig
import no.nav.k9.kafka.KafkaConfig
import java.time.Duration
import java.time.temporal.ChronoUnit

@KtorExperimentalAPI
internal data class Configuration(private val config : ApplicationConfig) {
    companion object {
        internal const val NAIS_STS_ALIAS = "nais-sts"
        internal const val AZURE_V2_ALIAS = "azure-v2"
    }

    private val clients = config.clients()

    internal fun issuers() = config.issuers().withoutAdditionalClaimRules()

    internal fun clients() = clients

    private fun azureClientConfigured() = clients().containsKey(AZURE_V2_ALIAS)

    internal fun hikariConfig() = createHikariConfig(
        jdbcUrl =  config.getRequiredString("nav.db.url", secret = false),
        username =  config.getRequiredString("nav.db.username", secret = false),
        password =  config.getRequiredString("nav.db.password", secret = true)
    )

    internal fun getKafkaConfig() =
        config.getRequiredString("nav.kafka.bootstrap_servers", secret = false).let { bootstrapServers ->
            val trustStore = config.getRequiredString("nav.trust_store.path", secret = false)?.let { trustStorePath ->
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
                exactlyOnce = trustStore != null,
                unreadyAfterStreamStoppedIn = unreadyAfterStreamStoppedIn()
            )
        }

    private fun unreadyAfterStreamStoppedIn() = Duration.of(
        config.getRequiredString("nav.kafka.unready_after_stream_stopped_in.amount", secret = false).toLong(),
        ChronoUnit.valueOf(config.getRequiredString("nav.kafka.unready_after_stream_stopped_in.unit", secret = false))
    )
}