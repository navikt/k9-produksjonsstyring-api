package no.nav.k9.domene.lager.oppgave

enum class BehandlingType (    val kode: String, val navn: String)  {
    FØRSTEGANGSSØKNAD("BT-002", "Førstegangsbehandling"),
    KLAGE("BT-003", "Klage"),
    REVURDERING("BT-004", "Revurdering"),
    SØKNAD("BT-005", "Søknad"),
    INNSYN("BT-006", "Innsyn"),
    ANKE("BT-008", "Anke");
}
