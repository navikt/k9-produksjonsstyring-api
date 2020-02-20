package no.nav.k9.domene.repository

import no.nav.k9.domene.lager.oppgave.AndreKriterierType
import no.nav.k9.domene.lager.oppgave.BehandlingStatus
import no.nav.k9.domene.lager.oppgave.OppgaveEventType
import no.nav.k9.integrasjon.Aksjonspunkt
import no.nav.k9.integrasjon.BehandlingK9sak
import no.nav.k9.kafka.dto.EventHendelse
import no.nav.k9.kafka.dto.Fagsystem
import no.nav.vedtak.felles.integrasjon.kafka.BehandlingProsessEventDto
import java.time.LocalDateTime
import java.util.*

data class BehandlingProsessEventer (
   //val uuid:UUID,
   val eventer: List<BehandlingProsessEventDto>
)


data class Event(
    val uuid: UUID,
    val fagsystem: Fagsystem,
    val saksnummer: String,
    val aktørId: String,

    val eventTid: LocalDateTime,
    val eventHendelse: EventHendelse,
    var behandlingStatus: BehandlingStatus,
    val behandlingSteg: String,
    val behandlendeEnhet: String,
    val ytelseTypeKode: String,
    val behandlingTypeKode: String,
    val opprettetBehandling: LocalDateTime,
    val aksjonspunktKoderMedStatusListe: Map<String, String>,
    val andreKriterierType: AndreKriterierType,
    var eventType: OppgaveEventType

)

data class Oppgave(
    val uuid: UUID,
    val fagsystem: Fagsystem,
    val saksnummer: String,
    val aktørId: String,

    val eventTid: LocalDateTime,
    val eventHendelse: EventHendelse,
    var behandlingStatus: BehandlingStatus,
    val behandlingSteg: String,
    val behandlendeEnhet: String,
    val ytelseTypeKode: String,
    val behandlingTypeKode: String,
    val opprettetBehandling: LocalDateTime,
    val aksjonspunktKoderMedStatusListe: Map<String, String>,
    val andreKriterierType: AndreKriterierType,
    var eventType: OppgaveEventType

)
//
//fun BehandlingProsessEventer.sisteOppgave(): Oppgave {
//    return this.oppgaver[this.oppgaver.lastIndex]
//}
//
fun BehandlingProsessEventer.sisteEvent(): BehandlingProsessEventDto {
    return this.eventer[this.eventer.lastIndex]
}

fun BehandlingProsessEventDto.aktiveAksjonspunkt(): Aksjonspunkter {
    return Aksjonspunkter(this.aksjonspunktKoderMedStatusListe.filter { entry -> entry.value == "OPPR" })
}

data class Aksjonspunkter(private val liste: Map<String, String>) {
    fun påVent(): Boolean {
       return this.liste.any { entry -> entry.key.startsWith("7") }
    }
    fun erTom(): Boolean {
        return this.liste.isEmpty()
    }
    fun tilBeslutter(): Boolean {
        return this.liste.any { entry -> entry.key == "5016" }
    }
}


//
//fun BehandlingProsessEventer.nestSisteOppgave(): Oppgave {
//    return this.oppgaver[this.oppgaver.lastIndex-1]
//}
//
//fun BehandlingProsessEventer.nestSisteEvent(): Event {
//    return this.eventer[this.eventer.lastIndex-1]
//}
//
//fun BehandlingProsessEventer.sisteBehandling(): BehandlingK9sak {
//    return this.behandlinger[this.behandlinger.lastIndex]
//}

fun BehandlingK9sak.åpneAksjonspunkt(): List<Aksjonspunkt> {
    return this.aksjonspunkter.filter(Aksjonspunkt::erAktiv)
}
fun AndreKriterierType.tilBeslutter(): Boolean {
    return this == AndreKriterierType.TIL_BESLUTTER
}
fun AndreKriterierType.papirsøknad(): Boolean {
    return this == AndreKriterierType.PAPIRSØKNAD
}