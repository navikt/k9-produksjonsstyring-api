package no.nav.k9.integrasjon.kafka

data class Metadata(
    val version: Int,
    val correlationId: String,
    val requestId: String
)