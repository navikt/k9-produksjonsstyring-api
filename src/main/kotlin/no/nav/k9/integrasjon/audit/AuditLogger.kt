package no.nav.k9.integrasjon.audit


import io.ktor.util.KtorExperimentalAPI
import no.nav.k9.Configuration
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class Auditlogger @KtorExperimentalAPI constructor(
    val configuration: Configuration,
    val isEnabled: Boolean = configuration.auditEnabled(),
    val defaultVendor: String = configuration.auditVendor(),
    val defaultProduct: String = configuration.auditProduct()
) {
    
    fun logg(auditdata: Auditdata) {
        if (isEnabled) {
            auditLogger.info(auditdata.toString())
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(Auditlogger::class.java)
        private val auditLogger: Logger = LoggerFactory.getLogger("auditLogger")
    }
}