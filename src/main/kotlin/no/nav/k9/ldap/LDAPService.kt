package no.nav.k9.ldap

import no.nav.sbl.util.EnvironmentUtils
import java.util.*
import javax.naming.NamingEnumeration
import javax.naming.NamingException
import javax.naming.directory.SearchControls
import javax.naming.directory.SearchResult

class LDAPService(private val ldapContextProvider: LdapContextProvider) {

    fun hentRollerForVeileder(ident: String): List<String> {
        val result = sokLDAP(ident)
        return getRoller(result)
    }

    private fun sokLDAP(ident: String): NamingEnumeration<SearchResult> {
        val searchbase =
            "OU=Users,OU=NAV,OU=BusinessUnits," + EnvironmentUtils.getRequiredProperty("LDAP_BASEDN")
        val searchCtrl = SearchControls()
        searchCtrl.searchScope = SearchControls.SUBTREE_SCOPE
        return try {
            ldapContextProvider.getInitialLdapContext
                .search(searchbase, String.format("(&(objectClass=user)(CN=%s))", ident), searchCtrl)
        } catch (e: NamingException) {
            throw RuntimeException(e)
        }
    }

    fun saksbehandlerHarRolle(ident: String, rolle: String): Boolean {
        val result = sokLDAP(ident)
        return getRoller(result).contains(rolle)
    }

    private fun getRoller(result: NamingEnumeration<SearchResult>): List<String> {
        return try {
            val attributes = result.next().attributes["memberof"].all
            parseRollerFraAD(attributes)
        } catch (e: NamingException) {
            throw RuntimeException(e)
        }
    }
    fun parseADRolle(rawRolleStrenger: List<String>): List<String> =
        rawRolleStrenger.map {
            check(it.startsWith("CN=")) { "Feil format på AD-rolle: $it" }
            it.split(",")[0].split("CN=")[1]
        }


    @Throws(NamingException::class)
    private fun parseRollerFraAD(attributes: NamingEnumeration<*>): List<String> {
        val rawRolleStrenger: MutableList<String> = ArrayList()
        while (attributes.hasMore()) {
            rawRolleStrenger.add(attributes.next() as String)
        }
        return parseADRolle(rawRolleStrenger)
    }
}