package no.nav.k9.domene.modell

import io.ktor.util.KtorExperimentalAPI
import no.nav.k9.aksjonspunktbehandling.eventresultat.EventResultat
import no.nav.k9.domene.lager.oppgave.BehandlingStatus
import no.nav.k9.domene.lager.oppgave.Oppgave
import no.nav.k9.integrasjon.gosys.*
import no.nav.k9.kafka.dto.BehandlingProsessEventDto
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class Modell(
    val eventer: List<BehandlingProsessEventDto>
) {

    @KtorExperimentalAPI
    fun syncOppgaveTilGosys(gosysOppgaveGateway: GosysOppgaveGateway): Oppgave {
        val event = sisteEvent()
        when (sisteEvent().aktiveAksjonspunkt().eventResultat()) {
            EventResultat.LUKK_OPPGAVE -> {
                avsluttOppgave(gosysOppgaveGateway, event)
            }
            EventResultat.LUKK_OPPGAVE_VENT -> {
                avsluttOppgave(gosysOppgaveGateway, event)
            }
            EventResultat.LUKK_OPPGAVE_MANUELT_VENT -> {
                avsluttOppgave(gosysOppgaveGateway, event)
            }
            EventResultat.GJENÅPNE_OPPGAVE -> {
                avsluttOppgave(gosysOppgaveGateway, event)
            }
            EventResultat.OPPRETT_BESLUTTER_OPPGAVE -> {
                if (finnesOppgaveIGosys(gosysOppgaveGateway, event)) {
                    opprettOppgave(gosysOppgaveGateway, event)
                }
            }
            EventResultat.OPPRETT_PAPIRSØKNAD_OPPGAVE -> {
                if (finnesOppgaveIGosys(gosysOppgaveGateway, event)) {
                    opprettOppgave(gosysOppgaveGateway, event)
                }
            }
            EventResultat.OPPRETT_OPPGAVE -> {
                if (finnesOppgaveIGosys(gosysOppgaveGateway, event)) {
                    opprettOppgave(gosysOppgaveGateway, event)
                }
            }
        }

        return oppgave()
    }

    @KtorExperimentalAPI
    private fun avsluttOppgave(
        gosysOppgaveGateway: GosysOppgaveGateway,
        event: BehandlingProsessEventDto
    ) {
        val gosysOppgave = finnOppgaveIGosys(gosysOppgaveGateway, event)[0]
        gosysOppgaveGateway.avsluttOppgave(
            AvsluttGosysOppgaveRequest(
                id = gosysOppgave.oppgaveId,
                versjon = gosysOppgave.versjon
            )
        )
    }

    @KtorExperimentalAPI
    private fun finnesOppgaveIGosys(
        gosysOppgaveGateway: GosysOppgaveGateway,
        event: BehandlingProsessEventDto
    ): Boolean {
        val oppgaver = finnOppgaveIGosys(gosysOppgaveGateway, event)
        return oppgaver.isEmpty()
    }

    @KtorExperimentalAPI
    private fun finnOppgaveIGosys(
        gosysOppgaveGateway: GosysOppgaveGateway,
        event: BehandlingProsessEventDto
    ): List<GosysOppgave> {
        return gosysOppgaveGateway.hentOppgaver(
            HentGosysOppgaverRequest(
                aktørId = event.aktørId,
                tema = GosysKonstanter.Tema.KAPITTEL_9_YTELSER,
                oppgaveType = GosysKonstanter.OppgaveType.BEHANDLE
            )
        )
    }


    @KtorExperimentalAPI
    private fun opprettOppgave(
        gosysOppgaveGateway: GosysOppgaveGateway,
        event: BehandlingProsessEventDto
    ) {
        gosysOppgaveGateway.opprettOppgave(
            OpprettGosysOppgaveRequest(
                oppgaveType = GosysKonstanter.OppgaveType.BEHANDLE,
                aktørId = event.aktørId,
                fagsakId = event.saksnummer,
                fagsaksystem = GosysKonstanter.Fagsaksystem.K9SAK,
                aktiv = LocalDate.now(),
                fristIDager = 1,
                prioritet = GosysKonstanter.Prioritet.NORMAL,
                temaGruppe = GosysKonstanter.TemaGruppe.FAMILIE,
                enhetsNummer = "enhetsNummer"
            )
        )
    }

    fun oppgave(): Oppgave {
        val event = sisteEvent()
        val eventResultat = sisteEvent().aktiveAksjonspunkt().eventResultat()
        var aktiv = true
        var oppgaveAvsluttet: LocalDateTime? = null
        var beslutterOppgave = false

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
            EventResultat.OPPRETT_PAPIRSØKNAD_OPPGAVE -> TODO()
            EventResultat.OPPRETT_OPPGAVE -> {
                aktiv = true
            }
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
            reservasjon = null,
            system = event.fagsystem.name,
            oppgaveEgenskap = emptyList(),
            aksjonspunkter = event.aktiveAksjonspunkt(),
            beslutterOppgave = beslutterOppgave
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

    fun avslutterSak(): Boolean {
        return false
    }
}

fun BehandlingProsessEventDto.aktiveAksjonspunkt(): Aksjonspunkter {
    return Aksjonspunkter(this.aksjonspunktKoderMedStatusListe.filter { entry -> entry.value == "OPPR" })
}

data class Aksjonspunkter(val liste: Map<String, String>) {
    fun lengde(): Int {
        return liste.size
    }

    fun påVent(): Boolean {
        return this.liste.any { entry -> entry.key.startsWith("7") }
    }

    fun erTom(): Boolean {
        return this.liste.isEmpty()
    }

    fun tilBeslutter(): Boolean {
        val tilBeslutter = listOf(
            "5031",
            "5038",
            "5039",
            "5042",
            "5046",
            "5047",
            "5049",
            "5050",
            "5052",
            "5053",
            "5058",
            "5072",
            "5074",
            "5076",
            "5077",
            "5078",
            "5079",
            "5089",
            "5090",
            "5095",
            "9001",
            "6005",
            "6007",
            "6011",
            "6012",
            "6014",
            "6015"
        )
        return this.liste.all { entry -> tilBeslutter.contains(entry.key) }
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
