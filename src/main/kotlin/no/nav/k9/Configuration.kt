package no.nav.k9

import io.ktor.config.ApplicationConfig
import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.dusseldorf.ktor.auth.clients
import no.nav.helse.dusseldorf.ktor.auth.issuers
import no.nav.helse.dusseldorf.ktor.auth.withoutAdditionalClaimRules
import no.nav.helse.dusseldorf.ktor.core.getRequiredList
import no.nav.helse.dusseldorf.ktor.core.getRequiredString
import java.net.URI

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
}