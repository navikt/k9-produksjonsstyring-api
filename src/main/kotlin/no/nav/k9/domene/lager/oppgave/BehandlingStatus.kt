package no.nav.k9.domene.lager.oppgave

enum class BehandlingStatus (val kode: String) {
    AVSLUTTET("AVSLU"),
    FATTER_VEDTAK("FVED"),
    IVERKSETTER_VEDTAK("IVED"),
    OPPRETTET("OPPRE"),
    UTREDES("UTRED");

    companion object {
        fun fraKode(kode: String): BehandlingStatus = values().find { it.kode == kode }!!
    }
}
