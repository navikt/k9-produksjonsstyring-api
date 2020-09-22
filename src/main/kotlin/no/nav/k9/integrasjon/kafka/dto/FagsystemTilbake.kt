package no.nav.k9.integrasjon.kafka.dto

import com.fasterxml.jackson.annotation.JsonFormat


@JsonFormat(shape = JsonFormat.Shape.OBJECT)
enum class FagsystemTilbake(val kode: String, val kodeverk: String) {
    K9SAK("K9SAK", "FAGSYSTEM"),
    K9TILBAKE("K9TILBAKE", "FAGSYSTEM"),
    FPTILBAKE("FPTILBAKE", "FAGSYSTEM");

    companion object {
        @JvmStatic
        fun fraKode(kode: String): FagsystemTilbake = values().find { it.kode == kode }!!
    }
}
