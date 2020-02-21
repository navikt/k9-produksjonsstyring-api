package no.nav.k9.domene.repository

import no.nav.k9.aksjonspunktbehandling.eventresultat.EventResultat
import no.nav.k9.domene.lager.oppgave.*
import no.nav.k9.domene.lager.oppgave.Oppgave
import no.nav.vedtak.felles.integrasjon.kafka.BehandlingProsessEventDto
import java.time.LocalDate
import java.time.LocalDateTime

data class Modell(
    val eventer: List<BehandlingProsessEventDto>
) {

    fun oppgave(): Oppgave {
        val event = sisteEvent()
        val eventResultat = sisteEvent().aktiveAksjonspunkt().eventResultat()
        var aktiv = true
        var oppgaveAvsluttet: LocalDateTime? = null

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
            EventResultat.OPPRETT_BESLUTTER_OPPGAVE -> TODO()
            EventResultat.OPPRETT_PAPIRSØKNAD_OPPGAVE -> TODO()
            EventResultat.OPPRETT_OPPGAVE -> TODO()
        }

        return Oppgave(
            behandlingId = event.behandlingId,
            fagsakSaksnummer = event.saksnummer,
            aktorId = event.aktørId.toLong(),
            behandlendeEnhet = event.behandlendeEnhet,
            behandlingType = BehandlingType.fraKode(event.behandlingTypeKode),
            fagsakYtelseType = FagsakYtelseType.fraKode(event.ytelseTypeKode),
            aktiv = aktiv,
            forsteStonadsdag = LocalDate.now(),
            utfortFraAdmin = false,
            behandlingsfrist = LocalDateTime.now(),
            behandlingStatus = BehandlingStatus.fraKode(event.behandlinStatus),
            eksternId = event.eksternId,
            behandlingOpprettet = event.opprettetBehandling,
            oppgaveAvsluttet = oppgaveAvsluttet,
            reservasjon = null,
            system = event.fagsystem.name,
            oppgaveEgenskap = emptyList()
        )
    }

    fun sisteEvent(): BehandlingProsessEventDto {
        return this.eventer[this.eventer.lastIndex]
    }
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
