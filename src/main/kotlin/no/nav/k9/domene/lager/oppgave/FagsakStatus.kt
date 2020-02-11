package no.nav.k9.domene.lager.oppgave

enum class FagsakStatus private constructor( val kode: String,  val navn: String)  {
    OPPRETTET("OPPR", "Opprettet"),
    UNDER_BEHANDLING("UBEH", "Under behandling"),
    LØPENDE("LOP", "Løpende"),
    AVSLUTTET("AVSLU", "Avsluttet");
}
