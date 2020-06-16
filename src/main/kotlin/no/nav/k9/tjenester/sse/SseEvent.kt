package no.nav.k9.tjenester.sse

data class SseEvent(val data: String, val event: String? = null, val id: String? = null)