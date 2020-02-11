package no.nav.k9.domene.lager.oppgave

import no.nav.k9.domene.organisasjon.Avdeling
import no.nav.k9.domene.organisasjon.Saksbehandler
import java.time.LocalDate

data class OppgaveFiltrering(
    val id: Long,
    val navn: String,
    val sortering: KøSortering,
    val filtreringBehandlingTyper: List<FiltreringBehandlingType>,
    val filtreringYtelseTyper: List<FiltreringYtelseType>,
    val filtreringAndreKriterierTyper: List<FiltreringAndreKriterierType>,
    val avdeling: Avdeling,
    val avdelingId: Long,
    val erDynamiskPeriode: Boolean,
    val fomDato: LocalDate,
    val tomDato: LocalDate,
    val fra: Long,
    val til: Long,
    val saksbehandlere: ArrayList<Saksbehandler>
)
