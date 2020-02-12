package no.nav.k9.domene.lager.oppgave

enum class FagsakYtelseType private constructor(val kode: String, val navn: String) {
    ENGANGSTØNAD("ES", "Engangsstønad"),
    FORELDREPENGER("FP", "Foreldrepenger"),
    SVANGERSKAPSPENGER("SVP", "Svangerskapspenger")
}
