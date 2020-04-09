package no.nav.k9.domene.modell

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat
import no.nav.k9.domene.lager.oppgave.Kodeverdi
import java.time.LocalDate
import java.util.*

data class OppgaveKø(
    val id: UUID,
    val navn: String,
    val sortering: KøSortering,
    val filtreringBehandlingTyper: List<BehandlingType>,
    val filtreringYtelseTyper: List<FagsakYtelseType>,
    val enhet: Enhet,
    val erDynamiskPeriode: Boolean,
    val fomDato: LocalDate,
    val tomDato: LocalDate,
    val saksbehandlere: List<Saksbehandler>
)

class Saksbehandler(
    val brukerIdent: String,
    val navn: String
)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
enum class Enhet(val navn: String) {
    VIKAFOSSEN("VIKAFOSSEN"),
    NASJONAL("NASJONAL")
}

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
enum class KøSortering(override val kode: String, val verdi: String, val felttype: String, val feltkategori: String) :
    Kodeverdi {
    BEHANDLINGSFRIST("BEHFRIST", "Dato for behandlingsfrist", "", ""),
    OPPRETT_BEHANDLING("OPPRBEH", "Dato for opprettelse av behandling", "", ""),
    FORSTE_STONADSDAG("FORSTONAD", "Dato for første stønadsdag", "", ""),
    BELØP("BELOP", "Beløp", "HELTALL", "TILBAKEKREVING"),
    FEILUTBETALINGSTART("FEILUTBETALINGSTART", "Dato for første feilutbetaling", "DATO", "TILBAKEKREVING");

    override val navn = ""
    override val kodeverk = "KO_SORTERING"
}

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
enum class AndreKriterierType(override val kode: String, override val navn: String) : Kodeverdi {
    TIL_BESLUTTER("TIL_BESLUTTER", "Til beslutter"),
    PAPIRSØKNAD("PAPIRSOKNAD", "Registrer papirsøknad"),
    UTBETALING_TIL_BRUKER("UTBETALING_TIL_BRUKER", "Utbetaling til bruker"),
    UTLANDSSAK("UTLANDSSAK", "Utland"),
    SOKT_GRADERING("SOKT_GRADERING", "Søkt gradering"),
    VURDER_SYKDOM("VURDER_SYKDOM", "Vurder sykdom"),
    VURDER_FARESIGNALER("VURDER_FARESIGNALER", "Vurder faresignaler");

    override val kodeverk = "ANDRE_KRITERIER_TYPE"

}

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
enum class FagsakYtelseType private constructor(override val kode: String, override val navn: String) : Kodeverdi {
    ENGANGSTØNAD("ES", "Engangsstønad"),
    FORELDREPENGER("FP", "Foreldrepenger"),
    SVANGERSKAPSPENGER("SVP", "Svangerskapspenger"),
    PLEIEPENGER_SYKT_BARN("PSB", "Pleiepenger sykt barn"),
    OMSORGSPENGER("OMP", "Omsorgspenger");

    override val kodeverk = "FAGSAK_YTELSE_TYPE"

    companion object {
        @JsonCreator
        @JvmStatic
        fun fraKode(kode: String): FagsakYtelseType = values().find { it.kode == kode }!!
    }
}

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
enum class BehandlingType(override val kode: String, override val navn: String) : Kodeverdi {
    FORSTEGANGSSOKNAD("BT-002", "Førstegangsbehandling"),
    KLAGE("BT-003", "Klage"),
    REVURDERING("BT-004", "Revurdering"),
    SOKNAD("BT-005", "Søknad"),
    INNSYN("BT-006", "Innsyn"),
    ANKE("BT-008", "Anke");

    override val kodeverk = "BEHANDLING_TYPE"

    companion object {
        @JsonCreator
        @JvmStatic
        fun fraKode(kode: String): BehandlingType = values().find { it.kode == kode }!!
    }

}




