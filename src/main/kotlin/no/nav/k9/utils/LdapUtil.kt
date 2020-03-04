package no.nav.k9.utils

import org.slf4j.LoggerFactory
import java.util.*
import javax.naming.InvalidNameException
import javax.naming.ldap.LdapName

class LdapUtil {
    private val logger = LoggerFactory.getLogger(LdapUtil::class.java)

    fun filtrerGrupper(grupper: Collection<String>): Collection<String> {
        return grupper.map { filterDNtoCNvalue(it) }
    }

    private fun filterDNtoCNvalue(value: String): String {
        if (value.toLowerCase(Locale.ROOT).contains("cn=")) {
            try {
                val ldapname =
                    LdapName(value) //NOSONAR, only used locally
                for (rdn in ldapname.rdns) {
                    if ("CN".equals(rdn.type, ignoreCase = true)) {
                        val cn = rdn.value.toString()
                        logger.debug(
                            "uid on DN form. Filtered from {} to {}",
                            value,
                            cn
                        ) //NOSONAR trusted source, validated SAML-token or LDAP memberOf
                        return cn
                    }
                }
            } catch (e: InvalidNameException) { //NOSONAR
                logger.debug(
                    "value not on DN form. Skipping filter. {}",
                    e.explanation
                ) //NOSONAR trusted source
            }
        }
        return value
    }
}