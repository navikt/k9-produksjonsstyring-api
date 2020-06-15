package no.nav.k9.integrasjon.abac

import java.time.LocalDateTime

class Cache {
    private val map =
        object : LinkedHashMap<String, CacheObject>(
        ) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CacheObject>): Boolean {
                return size > 1000
            }
        }

    fun set(key: String, value: CacheObject) {
        map[key] = value
    }

    fun remove(key: String) = map.remove(key)

    fun get(key: String): CacheObject? {
        val cacheObject = map[key] ?: return null
        if (cacheObject.expire.isBefore(LocalDateTime.now())) {
            remove(key)
            return null
        }
        return cacheObject
    }

    fun clear() {
        map.clear()
    }
}
