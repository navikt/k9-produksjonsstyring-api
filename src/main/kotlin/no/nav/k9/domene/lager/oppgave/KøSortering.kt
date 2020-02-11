package no.nav.k9.domene.lager.oppgave

enum class KøSortering (kode: String, verdi: String, felttype: String, feltkategori: String) {
    BEHANDLINGSFRIST("BEHFRIST", "Dato for behandlingsfrist", "", ""),
    OPPRETT_BEHANDLING("OPPRBEH", "Dato for opprettelse av behandling", "", ""),
    FORSTE_STONADSDAG("FORSTONAD", "Dato for første stønadsdag", "", ""),
    BELØP("BELOP", "Beløp", "HELTALL", "TILBAKEKREVING"),
    FEILUTBETALINGSTART("FEILUTBETALINGSTART", "Dato for første feilutbetaling", "DATO", "TILBAKEKREVING");
}
