package no.nav.k9.domene.modell

import no.nav.k9.domene.lager.oppgave.Oppgave
import no.nav.k9.kafka.dto.BehandlingProsessEventDto
import no.nav.k9.kafka.dto.EventHendelse
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class Modell(
    val eventer: List<BehandlingProsessEventDto>
) {
  
    fun oppgave(): Oppgave {
        val event = sisteEvent()
        val eventResultat = sisteEvent().aktiveAksjonspunkt().eventResultat()
        var aktiv = true
        var oppgaveAvsluttet: LocalDateTime? = null
        var beslutterOppgave = false
        var registrerPapir = false

        when (eventResultat) {
            EventResultat.LUKK_OPPGAVE -> {
                aktiv = false
                oppgaveAvsluttet = LocalDateTime.now()
            }
            EventResultat.LUKK_OPPGAVE_VENT -> {
                aktiv = false
                oppgaveAvsluttet = LocalDateTime.now()
            }
            EventResultat.LUKK_OPPGAVE_MANUELT_VENT -> {
                aktiv = false
                oppgaveAvsluttet = LocalDateTime.now()
            }
            EventResultat.GJENÅPNE_OPPGAVE -> TODO()
            EventResultat.OPPRETT_BESLUTTER_OPPGAVE -> {
                beslutterOppgave = true
            }
            EventResultat.OPPRETT_PAPIRSØKNAD_OPPGAVE -> {
                registrerPapir = true
            }
            EventResultat.OPPRETT_OPPGAVE -> {
                aktiv = true
            }
        }
        if (event.eventHendelse == EventHendelse.AKSJONSPUNKT_AVBRUTT || event.eventHendelse == EventHendelse.AKSJONSPUNKT_UTFØRT) {
            aktiv = false
        }
        var behandlingStatus = event.behandlingStatus
        behandlingStatus = behandlingStatus ?: BehandlingStatus.OPPRETTET.kode
        return Oppgave(
            behandlingId = event.behandlingId,
            fagsakSaksnummer = event.saksnummer,
            aktorId = event.aktørId,
            behandlendeEnhet = "event.behandlendeEnhet",
            behandlingType = BehandlingType.fraKode(event.behandlingTypeKode),
            fagsakYtelseType = FagsakYtelseType.fraKode(event.ytelseTypeKode),
            aktiv = aktiv,
            forsteStonadsdag = LocalDate.now(),
            utfortFraAdmin = false,
            behandlingsfrist = LocalDateTime.now().plusDays(1),
            behandlingStatus = BehandlingStatus.fraKode(behandlingStatus),
            eksternId = event.eksternId ?: UUID.randomUUID(),
            behandlingOpprettet = event.opprettetBehandling,
            oppgaveAvsluttet = oppgaveAvsluttet,
            system = event.fagsystem.name,
            oppgaveEgenskap = emptyList(),
            aksjonspunkter = event.aktiveAksjonspunkt(),
            utenlands = event.aktiveAksjonspunkt().aksjonspunkter.any { entry -> (entry.key == "5068" || entry.key == "6068") && entry.value != "AVBR" }, 
            tilBeslutter = beslutterOppgave,
            kombinert = false,
            registrerPapir = registrerPapir,
            selvstendigFrilans = false,
            søktGradering = false,
            utbetalingTilBruker = false,
            skjermet = false
        )
    }

    fun sisteEvent(): BehandlingProsessEventDto {
        return this.eventer[this.eventer.lastIndex]
    }

    fun starterSak(): Boolean {
        return this.eventer.size == 1
    }

    fun erTom(): Boolean {
        return this.eventer.isEmpty()
    }

}

fun BehandlingProsessEventDto.aktiveAksjonspunkt(): Aksjonspunkter {
    return Aksjonspunkter(this.aksjonspunktKoderMedStatusListe.filter { entry -> entry.value == "OPPR" })
}

data class Aksjonspunkter(val aksjonspunkter: Map<String, String>) {
    fun lengde(): Int {
        return aksjonspunkter.size
    }

    fun påVent(): Boolean {
        return this.aksjonspunkter.map { entry -> AksjonspunktDefinisjon.fraKode(entry.key) }.any{it.erAutopunkt()}
    }

    fun erTom(): Boolean {
        return this.aksjonspunkter.isEmpty()
    }

    fun tilBeslutter(): Boolean {        
        return this.aksjonspunkter.map { entry -> AksjonspunktDefinisjon.fraKode(entry.key) }.any{it.defaultTotrinnBehandling}
    }

    fun eventResultat(): EventResultat {
        if (erTom()) {
            return EventResultat.LUKK_OPPGAVE
        }

        if (påVent()) {
            return EventResultat.LUKK_OPPGAVE_VENT
        }

        if (tilBeslutter()) {
            return EventResultat.OPPRETT_BESLUTTER_OPPGAVE
        }

        return EventResultat.OPPRETT_OPPGAVE
    }
}
