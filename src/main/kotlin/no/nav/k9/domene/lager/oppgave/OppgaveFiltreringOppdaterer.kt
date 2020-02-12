package no.nav.k9.domene.lager.oppgave

import java.time.LocalDate
data class OppgaveFiltreringOppdaterer(
    val id: Long,
    val navn: String,
    val sortering: String,
    val erDynamiskPeriode: Boolean,
    val fomDato: LocalDate,
    val tomDato: LocalDate,
    val fra: Long,
    val til: Long
)