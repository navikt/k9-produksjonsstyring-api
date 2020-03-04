package no.nav.k9.domene.lager.oppgave

import no.nav.k9.domene.modell.AndreKriterierType
import no.nav.k9.domene.modell.BehandlingType
import no.nav.k9.domene.organisasjon.Avdeling
import no.nav.k9.domene.organisasjon.Saksbehandler
import java.time.LocalDate

data class OppgaveKø(
    val navn: String,
    val sortering: KøSortering,
    val filtreringBehandlingTyper: List<BehandlingType>,
    val filtreringYtelseTyper: List<FagsakYtelseType>,
    val filtreringAndreKriterierTyper: List<AndreKriterierType>,
    val avdeling: Avdeling,
    val avdelingId: Long,
    val erDynamiskPeriode: Boolean,
    val fomDato: LocalDate,
    val tomDato: LocalDate,
    val fra: Long,
    val til: Long,
    val saksbehandlere: ArrayList<Saksbehandler>
)
