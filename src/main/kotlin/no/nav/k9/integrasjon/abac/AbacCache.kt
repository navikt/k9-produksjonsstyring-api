package no.nav.k9.integrasjon.abac

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import java.util.*

private data class AbacCackeKey(val sub: String, val method: String, val action: String)

private data class AbacCacheValue(val timestamp: Long, val hasAccess: Boolean)

private val gson = GsonBuilder().create()

private const val EXPIRY_FIVE_MINUTES = 1000 * 60 * 5

class AbacCache(private val cacheExpiry: Int = EXPIRY_FIVE_MINUTES) {
    private val cache = mutableMapOf<AbacCackeKey, AbacCacheValue>()

    fun hasAccess(token: String, method: String, action: String): Boolean? {
        val cacheKey = AbacCackeKey(extractSubjectFromToken(token), method, action)
        val cacheValue = cache[cacheKey]

        if (cacheValue == null || cacheEntryExpired(cacheValue)) {
            cache.remove(cacheKey)
            return null
        }

        return cacheValue.hasAccess
    }

    fun storeResultOfLookup(token: String, method: String, action: String, access: Boolean) {
        val cacheKey = AbacCackeKey(extractSubjectFromToken(token), method, action)
        val cacheValue = AbacCacheValue(System.currentTimeMillis(), access)
        cache[cacheKey] = cacheValue
    }

    private fun cacheEntryExpired(cacheValue: AbacCacheValue): Boolean =
            (System.currentTimeMillis() - cacheValue.timestamp) > cacheExpiry

    private fun extractSubjectFromToken(token: String): String =
            gson.fromJson(String(Base64.getDecoder().decode(token)), JsonObject::class.java).get("sub").toString()
}