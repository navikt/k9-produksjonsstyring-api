package no.nav.k9.tjenester.fagsak

import com.google.common.math.IntMath
import no.nav.melding.virksomhet.behandlingsstatus.hendelsehandterer.v1.hendelseshandtererbehandlingsstatus.BehandlingStatus
import java.time.LocalDate

data class PersonDto (
  val navn: String,
  val personnummer: String,
  val kjoenn: String,
  val doedsdato: LocalDate?
)
