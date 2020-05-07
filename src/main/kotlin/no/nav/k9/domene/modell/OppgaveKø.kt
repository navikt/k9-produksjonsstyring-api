package no.nav.k9.domene.modell

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat
import no.nav.k9.domene.lager.oppgave.Kodeverdi
import no.nav.k9.domene.lager.oppgave.Oppgave
import no.nav.k9.domene.repository.ReservasjonRepository
import java.time.LocalDate
import java.util.*

data class OppgaveKø(
    val id: UUID,
    var navn: String,
    var sistEndret: LocalDate,
    var sortering: KøSortering,
    var filtreringBehandlingTyper: MutableList<BehandlingType>,
    var filtreringYtelseTyper: MutableList<FagsakYtelseType>,
    var filtreringAndreKriterierType: MutableList<AndreKriterierType>,
    val enhet: Enhet,
    var fomDato: LocalDate,
    var tomDato: LocalDate,
    var saksbehandlere: List<Saksbehandler>,
//    val tilBeslutter: Boolean,
//    val utbetalingTilBruker: Boolean,
//    val selvstendigFrilans: Boolean,
//    val kombinert: Boolean,
//    val søktGradering: Boolean,
//    val registrerPapir: Boolean,
//    val erPleiepenger: Boolean,
//    val erOmsorgspenger: Boolean,
//    val opprettBehandling: Boolean,
//    val førsteStønadsdag: Boolean
    var oppgaver: MutableList<UUID> = mutableListOf()
) {
    fun leggOppgaveTilEllerFjernFraKø(
        oppgave: Oppgave,
        reservasjonRepository: ReservasjonRepository
    ) {
        if (tilhørerOppgaveTilKø(oppgave = oppgave, reservasjonRepository = reservasjonRepository)) {
            if (!this.oppgaver.contains(oppgave.eksternId)) {
                this.oppgaver.add(oppgave.eksternId)
            }
        } else {
            this.oppgaver.remove(oppgave.eksternId)
        }
    }

    fun tilhørerOppgaveTilKø(
        oppgave: Oppgave,
        reservasjonRepository: ReservasjonRepository
    ): Boolean {
        if (erOppgavenReservert(reservasjonRepository, oppgave)) {
            return false
        }
        if (!erInnenforOppgavekøensPeriode(oppgave)) {
            return false
        }

        if (filtreringYtelseTyper.isNotEmpty() && !filtreringYtelseTyper.contains(oppgave.fagsakYtelseType)) {
            return false
        }

        if (filtreringYtelseTyper.isNotEmpty() && !filtreringBehandlingTyper.contains(oppgave.behandlingType)) {
            return false
        }

        if (filtreringAndreKriterierType.isEmpty()) {
            return true
        }

        if (oppgave.tilBeslutter && filtreringAndreKriterierType.contains(AndreKriterierType.TIL_BESLUTTER)) {
            return true
        }

        if (oppgave.registrerPapir && filtreringAndreKriterierType.contains(AndreKriterierType.PAPIRSØKNAD)) {
            return true
        }

        if (oppgave.utbetalingTilBruker && filtreringAndreKriterierType.contains(AndreKriterierType.UTBETALING_TIL_BRUKER)) {
            return true
        }

        if (oppgave.utenlands && filtreringAndreKriterierType.contains(AndreKriterierType.UTLANDSSAK)) {
            return true
        }

        if (oppgave.søktGradering && filtreringAndreKriterierType.contains(AndreKriterierType.SOKT_GRADERING)) {
            return true
        }

        if (oppgave.selvstendigFrilans && filtreringAndreKriterierType.contains(AndreKriterierType.SELVSTENDIG_FRILANS)) {
            return true
        }

        if (oppgave.kombinert && filtreringAndreKriterierType.contains(AndreKriterierType.KOMBINERT)) {
            return true
        }
        return false
    }

    private fun erInnenforOppgavekøensPeriode(oppgave: Oppgave): Boolean {
        if (oppgave.behandlingOpprettet.toLocalDate().isBefore(fomDato.plusDays(1))) {
            return false
        }

        if (oppgave.behandlingOpprettet.toLocalDate().isAfter(tomDato)) {
            return false
        }

        return true
    }

    private fun erOppgavenReservert(
        reservasjonRepository: ReservasjonRepository,
        oppgave: Oppgave
    ) = reservasjonRepository.hent().filter { it.erAktiv(reservasjonRepository) }
        .any() { it.oppgave == oppgave.eksternId }
}

class Saksbehandler(
    var brukerIdent: String?,
    var navn: String?,
    var epost: String
)

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
enum class Enhet(val navn: String) {
    VIKAFOSSEN("VIKAFOSSEN"),
    NASJONAL("NASJONAL");

    companion object {
        @JsonCreator
        @JvmStatic
        fun fraKode(navn: String): Enhet = values().find { it.navn == navn }!!
    }
}

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
enum class KøSortering(
    override val kode: String,
    override val navn: String,
    val felttype: String,
    val feltkategori: String
) :
    Kodeverdi {
    OPPRETT_BEHANDLING("OPPRBEH", "Dato for opprettelse av behandling", "DATO", ""),
    FORSTE_STONADSDAG("FORSTONAD", "Dato for første stønadsdag", "DATO", "");

    override val kodeverk = "KO_SORTERING"

    companion object {
        @JsonCreator
        @JvmStatic
        fun fraKode(kode: String): KøSortering = values().find { it.kode == kode }!!
    }
}

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
enum class AndreKriterierType(override val kode: String, override val navn: String) : Kodeverdi {
    TIL_BESLUTTER("TIL_BESLUTTER", "Til beslutter"),
    PAPIRSØKNAD("PAPIRSOKNAD", "Registrer papirsøknad"),
    UTBETALING_TIL_BRUKER("UTBETALING_TIL_BRUKER", "Utbetaling til bruker"),
    UTLANDSSAK("UTLANDSSAK", "Utland"),
    SOKT_GRADERING("SOKT_GRADERING", "Søkt gradering"),
    SELVSTENDIG_FRILANS("SELVSTENDIG_FRILANS", "Selvstendig næringsdrivende/frilans"),
    KOMBINERT("KOMBINERT", "Kombinert arbeidstaker - selvstendig/frilans");

    override val kodeverk = "ANDRE_KRITERIER_TYPE"

    companion object {
        @JsonCreator
        @JvmStatic
        fun fraKode(kode: String): AndreKriterierType = values().find { it.kode == kode }!!
    }
}

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
enum class FagsakYtelseType private constructor(override val kode: String, override val navn: String) : Kodeverdi {
    PLEIEPENGER_SYKT_BARN("PSB", "Pleiepenger sykt barn"),
    OMSORGSPENGER("OMP", "Omsorgspenger"),
    FRISINN("FRISINN", "Frisinn");

    override val kodeverk = "FAGSAK_YTELSE_TYPE"

    companion object {
        @JsonCreator
        @JvmStatic
        fun fraKode(kode: String): FagsakYtelseType = values().find { it.kode == kode }!!
    }
}

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
enum class FagsakStatus(override val kode: String, override val navn: String) : Kodeverdi {
    OPPRETTET("OPPR", "Opprettet"),
    UNDER_BEHANDLING("UBEH", "Under behandling"),
    LØPENDE("LOP", "Løpende"),
    AVSLUTTET("AVSLU", "Avsluttet");

    override val kodeverk = "FAGSAK_STATUS"

    companion object {
        @JsonCreator
        @JvmStatic
        fun fraKode(kode: String): FagsakStatus = values().find { it.kode == kode }!!
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

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
enum class BehandlingStatus(override val kode: String) : Kodeverdi {
    AVSLUTTET("AVSLU"),
    FATTER_VEDTAK("FVED"),
    IVERKSETTER_VEDTAK("IVED"),
    OPPRETTET("OPPRE"),
    UTREDES("UTRED");

    override val kodeverk = "BEHANDLING_TYPE"
    override val navn = ""

    companion object {
        @JsonCreator
        @JvmStatic
        fun fraKode(kode: String): BehandlingStatus = values().find { it.kode == kode }!!
    }
}




