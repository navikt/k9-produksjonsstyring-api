package no.nav.k9.domene.modell

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat
import no.nav.k9.domene.lager.oppgave.Kodeverdi
import no.nav.k9.domene.lager.oppgave.Oppgave
import no.nav.k9.domene.repository.ReservasjonRepository
import no.nav.k9.tjenester.avdelingsleder.oppgaveko.AndreKriterierDto
import no.nav.k9.tjenester.saksbehandler.nokkeltall.NyeOgFerdigstilteOppgaver
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class OppgaveIdMedDato(val id: UUID, val dato: LocalDateTime)

data class OppgaveKø(
    val id: UUID,
    var navn: String,
    var sistEndret: LocalDate,
    var sortering: KøSortering,
    var filtreringBehandlingTyper: MutableList<BehandlingType>,
    var filtreringYtelseTyper: MutableList<FagsakYtelseType>,
    var filtreringAndreKriterierType: MutableList<AndreKriterierDto>,
    val enhet: Enhet,
    var fomDato: LocalDate?,
    var tomDato: LocalDate?,
    var saksbehandlere: MutableList<Saksbehandler>,
    var skjermet: Boolean = false,
    var oppgaverOgDatoer: MutableList<OppgaveIdMedDato> = mutableListOf(),

    var nyeOgFerdigstilteOppgaver: MutableMap<LocalDate, MutableMap<String, NyeOgFerdigstilteOppgaver>> = mutableMapOf()
) {

    fun leggOppgaveTilEllerFjernFraKø(
        oppgave: Oppgave,
        reservasjonRepository: ReservasjonRepository
    ): Boolean {
        if (tilhørerOppgaveTilKø(oppgave = oppgave, reservasjonRepository = reservasjonRepository)) {
            if (this.oppgaverOgDatoer.none { it.id == oppgave.eksternId }) {
                this.oppgaverOgDatoer.add(
                    OppgaveIdMedDato(
                        oppgave.eksternId,
                        if (sortering == KøSortering.OPPRETT_BEHANDLING) {
                            oppgave.behandlingOpprettet
                        } else {
                            oppgave.forsteStonadsdag.atStartOfDay()
                        }
                    )
                )
                return true
            }

        } else {
            if (this.oppgaverOgDatoer.any { it.id == oppgave.eksternId }) {
                this.oppgaverOgDatoer.remove(this.oppgaverOgDatoer.first { it.id == oppgave.eksternId })
                return true
            }
        }
        return false
    }

    fun nyeOgFerdigstilteOppgaver(oppgave: Oppgave): NyeOgFerdigstilteOppgaver {
        return nyeOgFerdigstilteOppgaver.getOrPut(oppgave.eventTid.toLocalDate()) {
            mutableMapOf()
        }.getOrPut(oppgave.behandlingType.kode) {
            NyeOgFerdigstilteOppgaver(
                behandlingType = oppgave.behandlingType,
                dato = oppgave.eventTid.toLocalDate()
            )
        }
    }

    fun tilhørerOppgaveTilKø(
        oppgave: Oppgave,
        reservasjonRepository: ReservasjonRepository,
        taHensynTilReservasjon: Boolean = true
    ): Boolean {
        if (!oppgave.aktiv) {
            return false
        }

        if (taHensynTilReservasjon && erOppgavenReservert(reservasjonRepository, oppgave)) {
            return false
        }
        if (!erInnenforOppgavekøensPeriode(oppgave)) {
            return false
        }

        if (filtreringYtelseTyper.isNotEmpty() && !filtreringYtelseTyper.contains(oppgave.fagsakYtelseType)) {
            return false
        }

        if (filtreringBehandlingTyper.isNotEmpty() && !filtreringBehandlingTyper.contains(oppgave.behandlingType)) {
            return false
        }

        if (oppgave.skjermet != this.skjermet) {
            return false
        }

        if (filtreringAndreKriterierType.isEmpty()) {
            return true
        }

        if (ekskluderer(oppgave)) {
            return false
        }

        if (filtreringAndreKriterierType.none { it.inkluder }) {
            return true
        }

        if (inkluderer(oppgave)) {
            return true
        }

        return false
    }

    fun nyeOgFerdigstilteOppgaverPerAntallDager(antallDager: Int): List<NyeOgFerdigstilteOppgaver> {
        return nyeOgFerdigstilteOppgaver.values.flatMap { it.values }.sortedByDescending { it.dato }.take(antallDager)
    }

    private fun erInnenforOppgavekøensPeriode(oppgave: Oppgave): Boolean {
        if (sortering == KøSortering.OPPRETT_BEHANDLING) {
            if (fomDato != null && oppgave.behandlingOpprettet.toLocalDate().isBefore(fomDato!!.plusDays(1))) {
                return false
            }

            if (tomDato != null && oppgave.behandlingOpprettet.toLocalDate().isAfter(tomDato)) {
                return false
            }
        }

        if (sortering == KøSortering.FORSTE_STONADSDAG) {
            if (fomDato != null && oppgave.forsteStonadsdag.isBefore(fomDato!!.plusDays(1))) {
                return false
            }

            if (tomDato != null && oppgave.forsteStonadsdag.isAfter(tomDato)) {
                return false
            }
        }
        return true
    }

    private fun inkluderer(oppgave: Oppgave): Boolean {
        val inkluderKriterier = filtreringAndreKriterierType.filter { it.inkluder }
        return sjekkOppgavensKriterier(oppgave, inkluderKriterier)
    }

    private fun ekskluderer(oppgave: Oppgave): Boolean {
        val ekskluderKriterier = filtreringAndreKriterierType.filter { !it.inkluder }
        return sjekkOppgavensKriterier(oppgave, ekskluderKriterier)
    }

    private fun sjekkOppgavensKriterier(oppgave: Oppgave, kriterier: List<AndreKriterierDto>): Boolean {
        if (oppgave.tilBeslutter && kriterier.map { it.andreKriterierType }
                .contains(AndreKriterierType.TIL_BESLUTTER)) {
            return true
        }

        if (oppgave.registrerPapir && kriterier.map { it.andreKriterierType }
                .contains(AndreKriterierType.PAPIRSØKNAD)) {
            return true
        }

        if (oppgave.årskvantum && kriterier.map { it.andreKriterierType }
                .contains(AndreKriterierType.AARSKVANTUM)) {
            return true
        }

        if (oppgave.avklarMedlemskap && kriterier.map { it.andreKriterierType }
                .contains(AndreKriterierType.AVKLAR_MEDLEMSKAP)) {
            return true
        }
        if (oppgave.vurderopptjeningsvilkåret && kriterier.map { it.andreKriterierType }
                .contains(AndreKriterierType.VURDER_OPPTJENINGSVILKÅRET)) {
            return true
        }

        if (oppgave.utbetalingTilBruker && kriterier.map { it.andreKriterierType }
                .contains(AndreKriterierType.UTBETALING_TIL_BRUKER)) {
            return true
        }

        if (oppgave.utenlands && kriterier.map { it.andreKriterierType }.contains(AndreKriterierType.UTLANDSSAK)) {
            return true
        }

        if (oppgave.søktGradering && kriterier.map { it.andreKriterierType }
                .contains(AndreKriterierType.SOKT_GRADERING)) {
            return true
        }

        if (oppgave.selvstendigFrilans && kriterier.map { it.andreKriterierType }
                .contains(AndreKriterierType.SELVSTENDIG_FRILANS)) {
            return true
        }

        if (oppgave.kombinert && kriterier.map { it.andreKriterierType }.contains(AndreKriterierType.KOMBINERT)) {
            return true
        }
        return false
    }

    private fun erOppgavenReservert(
        reservasjonRepository: ReservasjonRepository,
        oppgave: Oppgave
    ): Boolean {
        if (reservasjonRepository.finnes(oppgave.eksternId)) {
            val reservasjon = reservasjonRepository.hent(oppgave.eksternId)
            return reservasjon.erAktiv()
        }
        return false
    }

}

class Saksbehandler(
    var brukerIdent: String?,
    var navn: String?,
    var epost: String,
    var reservasjoner : MutableSet<UUID> = mutableSetOf(),
    var enhet : String?
)

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
enum class Enhet(val navn: String) {
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
    KOMBINERT("KOMBINERT", "Kombinert arbeidstaker - selvstendig/frilans"),
    AARSKVANTUM("AARSKVANTUM", "Årskvantum"),
    AVKLAR_MEDLEMSKAP("AVKLAR_MEDLEMSKAP", "Avklar medlemskap"),
    VURDER_OPPTJENINGSVILKÅRET("VURDER_OPPTJENINGSVILKÅRET", "Avklar opptjeningsvilkåret");

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
  //  PPN("PPN", "PPN"),
  //  OLP("OLP", "OLP");

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
enum class BehandlingType(override val kode: String, override val navn: String, override val kodeverk: String) :
    Kodeverdi {
    FORSTEGANGSSOKNAD("BT-002", "Førstegangsbehandling", "ae0034"),
    KLAGE("BT-003", "Klage", "ae0058"),
    REVURDERING("BT-004", "Revurdering", "ae0028"),
    INNSYN("BT-006", "Innsyn", "ae0042"),
    ANKE("BT-008", "Anke", "ae0046");

    companion object {
        @JsonCreator
        @JvmStatic
        fun fraKode(kode: String): BehandlingType = values().find { it.kode == kode }?:FORSTEGANGSSOKNAD
    }

}

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
enum class BehandlingStatus(override val kode: String, override val navn: String) : Kodeverdi {
    AVSLUTTET("AVSLU", "Avsluttet"),
    FATTER_VEDTAK("FVED", "Fatter vedtak"),
    IVERKSETTER_VEDTAK("IVED", "Iverksetter vedtak"),
    OPPRETTET("OPPRE", "Opprettet"),
    UTREDES("UTRED", "Utredes");

    override val kodeverk = "BEHANDLING_TYPE"

    companion object {
        @JsonCreator
        @JvmStatic
        fun fraKode(kode: String): BehandlingStatus = values().find { it.kode == kode }?:OPPRETTET
    }
}




