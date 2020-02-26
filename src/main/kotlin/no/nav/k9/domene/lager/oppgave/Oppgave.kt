package no.nav.k9.domene.lager.oppgave

import no.nav.k9.domene.repository.Aksjonspunkter
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class Oppgave(
    val behandlingId: Long,
    val fagsakSaksnummer: String,
    val aktorId: String,
    val behandlendeEnhet: String,
    val behandlingsfrist: LocalDateTime,
    val behandlingOpprettet: LocalDateTime,
    val forsteStonadsdag: LocalDate,
    var behandlingStatus: BehandlingStatus,
    val behandlingType: BehandlingType,
    val fagsakYtelseType: FagsakYtelseType,
    val aktiv: Boolean,
    val system: String,
    val oppgaveAvsluttet: LocalDateTime?,
    val utfortFraAdmin: Boolean,
    val eksternId: UUID,
    val reservasjon: Reservasjon?,
    val oppgaveEgenskap: List<OppgaveEgenskap>,
    val beslutterOppgave: Boolean,
    val aksjonspunkter: Aksjonspunkter
)