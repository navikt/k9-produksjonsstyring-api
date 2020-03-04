package no.nav.k9.domene.lager.oppgave

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*
import java.util.function.Function
import java.util.function.Predicate
import java.util.function.Supplier

enum class FagsakYtelseType(override val kode: String, override val navn: String): Kodeverdi {
    ENGANGSTØNAD("ES", "Engangsstønad"),
    FORELDREPENGER("FP", "Foreldrepenger"),
    SVANGERSKAPSPENGER("SVP", "Svangerskapspenger"),
    PLEIEPENGER_SYKT_BARN("PSB", "Svangerskapspenger");

    override val kodeverk = "FAGSAK_YTELSE_TYPE"

    companion object {
        fun fraKode(kode: String): FagsakYtelseType = values().find { it.kode == kode }!!
    }
}
