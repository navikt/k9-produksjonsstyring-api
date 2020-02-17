package no.nav.k9.tjenester.avdelingsleder

data class Avdeling (
    val id: Long,
    val avdelingEnhet: String,
    val navn: String,
    val kreverKode6: Boolean)