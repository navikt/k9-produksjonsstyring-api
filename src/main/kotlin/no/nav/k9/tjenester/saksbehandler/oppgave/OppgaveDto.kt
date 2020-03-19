package no.nav.k9.tjenester.saksbehandler.oppgave

import no.nav.k9.domene.lager.oppgave.BehandlingStatus
import no.nav.k9.domene.modell.BehandlingType
import no.nav.k9.domene.modell.FagsakYtelseType
import java.time.LocalDateTime
import java.util.*

class OppgaveDto(
    val status: OppgaveStatusDto,
    val behandlingId: Long,
    val saksnummer: Long,
    val navn: String,
    val system: String,
    val personnummer: String,
    val behandlingstype: BehandlingType,
    val fagsakYtelseType: FagsakYtelseType,
    val behandlingStatus: BehandlingStatus,
    val erTilSaksbehandling: Boolean,
    val opprettetTidspunkt: LocalDateTime,
    val behandlingsfrist: LocalDateTime,
    val eksternId: UUID)