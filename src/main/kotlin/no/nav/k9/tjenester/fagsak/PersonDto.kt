package no.nav.k9.tjenester.fagsak
import java.time.LocalDate

data class PersonDto (
  val navn: String,
  val personnummer: String,
  val kjoenn: String,
  val doedsdato: LocalDate?
)
