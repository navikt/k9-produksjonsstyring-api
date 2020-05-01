package no.nav.k9.integrasjon.abac

import com.google.gson.GsonBuilder
import no.nav.k9.tjenester.saksbehandler.IdToken

private data class AbacCackeKey(val sub: String, val method: String, val action: String)

private data class AbacCacheValue(val timestamp: Long, val hasAccess: Boolean)

private val gson = GsonBuilder().create()

private const val EXPIRY_FIVE_MINUTES = 1000 * 60 * 5

class AbacCache(private val cacheExpiry: Int = EXPIRY_FIVE_MINUTES) {
    private val cache = mutableMapOf<AbacCackeKey, AbacCacheValue>()

    fun hasAccess(token: IdToken, method: String, action: String): Boolean? {
        val cacheKey = AbacCackeKey(token.ident.value, method, action)
        val cacheValue = cache[cacheKey]

        if (cacheValue == null || cacheEntryExpired(cacheValue)) {
            cache.remove(cacheKey)
            return null
        }

        return cacheValue.hasAccess
    }

    fun storeResultOfLookup(token: IdToken, method: String, action: String, access: Boolean) {
        val cacheKey = AbacCackeKey(token.ident.value, method, action)
        val cacheValue = AbacCacheValue(System.currentTimeMillis(), access)
        cache[cacheKey] = cacheValue
    }

    private fun cacheEntryExpired(cacheValue: AbacCacheValue): Boolean =
            (System.currentTimeMillis() - cacheValue.timestamp) > cacheExpiry
    
}