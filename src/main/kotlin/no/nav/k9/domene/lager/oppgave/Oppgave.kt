package no.nav.k9.domene.lager.oppgave

import no.nav.k9.domene.modell.Aksjonspunkter
import no.nav.k9.domene.modell.BehandlingStatus
import no.nav.k9.domene.modell.BehandlingType
import no.nav.k9.domene.modell.FagsakYtelseType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class Oppgave(
    val behandlingId: Long?,
    val fagsakSaksnummer: String,
    val aktorId: String,
    val behandlendeEnhet: String,
    val behandlingsfrist: LocalDateTime,
    val behandlingOpprettet: LocalDateTime,
    val forsteStonadsdag: LocalDate,
    var behandlingStatus: BehandlingStatus,
    val behandlingType: BehandlingType,
    val fagsakYtelseType: FagsakYtelseType,
    val eventTid: LocalDateTime = LocalDateTime.now(),
    val aktiv: Boolean,
    val system: String,
    val oppgaveAvsluttet: LocalDateTime?,
    val utfortFraAdmin: Boolean,
    val eksternId: UUID,
    val oppgaveEgenskap: List<OppgaveEgenskap>,
    val aksjonspunkter: Aksjonspunkter,
    val tilBeslutter: Boolean,
    val utbetalingTilBruker: Boolean,
    val selvstendigFrilans: Boolean,
    val kombinert: Boolean,
    val søktGradering: Boolean,
    val registrerPapir: Boolean,
    val årskvantum: Boolean,
    val avklarMedlemskap: Boolean,
    var kode6: Boolean,
    val utenlands: Boolean,
    val vurderopptjeningsvilkåret : Boolean = false
){
    fun avluttet(): Boolean {
        return behandlingStatus == BehandlingStatus.AVSLUTTET
    }
}
