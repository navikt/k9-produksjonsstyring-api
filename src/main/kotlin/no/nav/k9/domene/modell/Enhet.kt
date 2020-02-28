package no.nav.k9.domene.modell

import java.time.LocalDate

data class Enhet(
    val avdelingEnhet: String,
    val navn: String,
    val oppgaveFiltrering: List<OppgaveFiltrering>
) {
}

data class OppgaveFiltrering(
    val navn: String,
    val sortering: KøSortering,
    val filtreringBehandlingTyper: List<BehandlingType>,
    val filtreringYtelseTyper: List<FagsakYtelseType>,
    val filtreringAndreKriterierTyper: List<AndreKriterierType>,
    val enhet: Enhet,
    val avdelingId: Long,
    val erDynamiskPeriode: Boolean,
    val fomDato: LocalDate,
    val tomDato: LocalDate,
    val fra: Long,
    val til: Long,
    val saksbehandlere: List<Saksbehandler>
)

data class Saksbehandler(val saksbehandlerIdent: String) {

}

enum class KøSortering(kode: String, verdi: String, felttype: String, feltkategori: String) {
    BEHANDLINGSFRIST("BEHFRIST", "Dato for behandlingsfrist", "", ""),
    OPPRETT_BEHANDLING("OPPRBEH", "Dato for opprettelse av behandling", "", ""),
    FORSTE_STONADSDAG("FORSTONAD", "Dato for første stønadsdag", "", ""),
    BELØP("BELOP", "Beløp", "HELTALL", "TILBAKEKREVING"),
    FEILUTBETALINGSTART("FEILUTBETALINGSTART", "Dato for første feilutbetaling", "DATO", "TILBAKEKREVING");
}

enum class AndreKriterierType(kode: String, navn: String) {
    TIL_BESLUTTER("TIL_BESLUTTER", "Til beslutter"),
    PAPIRSØKNAD("PAPIRSOKNAD", "Registrer papirsøknad"),
    UTBETALING_TIL_BRUKER("UTBETALING_TIL_BRUKER", "Utbetaling til bruker"),
    UTLANDSSAK("UTLANDSSAK", "Utland"),
    SOKT_GRADERING("SOKT_GRADERING", "Søkt gradering"),
    VURDER_SYKDOM("VURDER_SYKDOM", "Vurder sykdom"),
    VURDER_FARESIGNALER("VURDER_FARESIGNALER", "Vurder faresignaler");
}

enum class FagsakYtelseType private constructor(val kode: String, val navn: String) {
    ENGANGSTØNAD("ES", "Engangsstønad"),
    FORELDREPENGER("FP", "Foreldrepenger"),
    SVANGERSKAPSPENGER("SVP", "Svangerskapspenger"),
    PLEIEPENGER_SYKT_BARN("PSB", "Svangerskapspenger");

    companion object {
        fun fraKode(kode: String): FagsakYtelseType = values().find { it.kode == kode }!!
    }
}

enum class BehandlingType(val kode: String, val navn: String) {
    FØRSTEGANGSSØKNAD("BT-002", "Førstegangsbehandling"),
    KLAGE("BT-003", "Klage"),
    REVURDERING("BT-004", "Revurdering"),
    SØKNAD("BT-005", "Søknad"),
    INNSYN("BT-006", "Innsyn"),
    ANKE("BT-008", "Anke");

    companion object {
        fun fraKode(kode: String): BehandlingType = values().find { it.kode == kode }!!
    }
}
