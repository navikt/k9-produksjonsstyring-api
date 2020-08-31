package no.nav.k9.integrasjon.pdl

data class PdlResponse(
        val ikkeTilgang: Boolean,
        val aktorId: AktøridPdl?
)
