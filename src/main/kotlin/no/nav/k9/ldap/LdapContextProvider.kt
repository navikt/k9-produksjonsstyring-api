package no.nav.k9.ldap

import no.nav.sbl.util.EnvironmentUtils
import java.util.*
import javax.naming.Context
import javax.naming.NamingException
import javax.naming.ldap.InitialLdapContext
import javax.naming.ldap.LdapContext

class LdapContextProvider {
    companion object {
        private val env =
            Hashtable<String, String>()
        const val LDAP_USERNAME = "LDAP_USERNAME"
        const val LDAP_PASSWORD = "LDAP_PASSWORD"

        init {
            env[Context.INITIAL_CONTEXT_FACTORY] = "com.sun.jndi.ldap.LdapCtxFactory"
            env[Context.SECURITY_AUTHENTICATION] = "simple"
            env[Context.PROVIDER_URL] = EnvironmentUtils.getRequiredProperty("LDAP_URL")
            env[Context.SECURITY_PRINCIPAL] = EnvironmentUtils.getRequiredProperty(LDAP_USERNAME)
            env[Context.SECURITY_CREDENTIALS] = EnvironmentUtils.getRequiredProperty(LDAP_PASSWORD)
        }
    }

    val getInitialLdapContext: LdapContext = try {
            InitialLdapContext(env, null)
        } catch (e: NamingException) {
            throw RuntimeException(e)
        }
}