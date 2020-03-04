package no.nav.k9.domene.lager.oppgave

enum class FagsakStatus(override val kode: String, override val navn: String): Kodeverdi  {
    OPPRETTET("OPPR", "Opprettet"),
    UNDER_BEHANDLING("UBEH", "Under behandling"),
    LØPENDE("LOP", "Løpende"),
    AVSLUTTET("AVSLU", "Avsluttet");

    override val kodeverk = "FAGSAK_STATUS"
}
