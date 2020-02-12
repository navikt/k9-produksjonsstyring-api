package no.nav.k9.aksjonspunktbehandling

import no.nav.k9.aksjonspunktbehandling.eventresultat.EventResultat
import no.nav.k9.aksjonspunktbehandling.eventresultat.K9SakEventMapper
import no.nav.k9.domene.lager.oppgave.AndreKriterierType
import no.nav.k9.domene.lager.oppgave.Oppgave
import no.nav.k9.domene.lager.oppgave.OppgaveEventLogg
import no.nav.k9.domene.lager.oppgave.OppgaveEventType
import no.nav.k9.domene.repository.OppgaveRepository
import no.nav.k9.integrasjon.Aksjonspunkt
import no.nav.k9.integrasjon.BehandlingK9sak
import no.nav.k9.integrasjon.K9SakRestKlient
import no.nav.vedtak.felles.integrasjon.kafka.BehandlingProsessEventDto
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

class K9sakEventHandler() {

    private val log = LoggerFactory.getLogger(K9sakEventHandler::class.java)

    fun prosesser(event: BehandlingProsessEventDto) {
        val behandlingId = event.behandlingId
        val behandling = K9SakRestKlient().getBehandling(behandlingId)

        val eksternId = behandling.uuid

        val tidligereEventer = OppgaveRepository().hentEventer(eksternId)
        val aksjonspunkter = behandling.aksjonspunkter
        val oppgaveEgenskapFinner = OppgaveEgenskapFinner(behandling, tidligereEventer, aksjonspunkter)

        val eventResultat =
            K9SakEventMapper().signifikantEventFra(aksjonspunkter, tidligereEventer, event.behandlendeEnhet)

        when (eventResultat) {
            EventResultat.LUKK_OPPGAVE -> {
                log.info("Lukker oppgave")
                avsluttOppgaveOgLoggEvent(eksternId, event, eventType = OppgaveEventType.LUKKET, frist = null)
            }
            EventResultat.LUKK_OPPGAVE_VENT -> {
                log.info("Behandling satt automatisk på vent, lukker oppgave.")
                avsluttOppgaveOgLoggEvent(
                    eksternId,
                    event,
                    OppgaveEventType.VENT,
                    finnVentAksjonspunktFrist(aksjonspunkter)
                )
            }
            EventResultat.LUKK_OPPGAVE_MANUELT_VENT -> {
                log.info("Behandling satt manuelt på vent, lukker oppgave.")
                avsluttOppgaveOgLoggEvent(
                    eksternId,
                    event,
                    OppgaveEventType.MANU_VENT,
                    finnManuellAksjonspunktFrist(aksjonspunkter)
                )
            }
            EventResultat.GJENÅPNE_OPPGAVE -> {
                log.info("Gjenåpner oppgave")
                val gjenåpneOppgave = OppgaveRepository().gjenåpneOppgave(event.eksternId);
                loggEvent(
                    behandlingId = behandlingId,
                    eksternId = gjenåpneOppgave.eksternId,
                    oppgaveEventType = OppgaveEventType.GJENAPNET,
                    andreKriterierType = null,
                    behandlendeEnhet = event.behandlendeEnhet
                )
                OppgaveEgenskapHandler().håndterOppgaveEgenskaper(gjenåpneOppgave, oppgaveEgenskapFinner)
            }
            EventResultat.OPPRETT_BESLUTTER_OPPGAVE -> {
                avsluttOppgaveHvisÅpen(behandlingId, eksternId, tidligereEventer, event.behandlendeEnhet)
                val oppgave = nyOppgave(eksternId, event, behandling)
                reserverOppgaveFraTidligereReservasjon(null, oppgave.id)
                loggEvent(
                    behandlingId = behandlingId,
                    eksternId = oppgave.eksternId,
                    oppgaveEventType = OppgaveEventType.OPPRETTET,
                    andreKriterierType = AndreKriterierType.TIL_BESLUTTER,
                    behandlendeEnhet = event.behandlendeEnhet
                )
                OppgaveEgenskapHandler().håndterOppgaveEgenskaper(oppgave, oppgaveEgenskapFinner)
            }
            EventResultat.OPPRETT_PAPIRSØKNAD_OPPGAVE -> {
                avsluttOppgaveHvisÅpen(behandlingId, eksternId, tidligereEventer, event.behandlendeEnhet)
                val oppgave = nyOppgave(eksternId, event, behandling)
                reserverOppgaveFraTidligereReservasjon(null, oppgave.id)
                loggEvent(
                    behandlingId = behandlingId,
                    eksternId = oppgave.eksternId,
                    oppgaveEventType = OppgaveEventType.OPPRETTET,
                    andreKriterierType = AndreKriterierType.PAPIRSØKNAD,
                    behandlendeEnhet = event.behandlendeEnhet
                )
                OppgaveEgenskapHandler().håndterOppgaveEgenskaper(oppgave, oppgaveEgenskapFinner)
            }
            EventResultat.OPPRETT_OPPGAVE -> {
                avsluttOppgaveHvisÅpen(behandlingId, eksternId, tidligereEventer, event.behandlendeEnhet)
                val oppgave = nyOppgave(eksternId, event, behandling)
                reserverOppgaveFraTidligereReservasjon(null, oppgave.id)
                loggEvent(
                    behandlingId = behandlingId,
                    eksternId = oppgave.eksternId,
                    oppgaveEventType = OppgaveEventType.OPPRETTET,
                    andreKriterierType = null,
                    behandlendeEnhet = event.behandlendeEnhet
                )
                OppgaveEgenskapHandler().håndterOppgaveEgenskaper(oppgave, oppgaveEgenskapFinner)
            }
        }
    }

    private fun reserverOppgaveFraTidligereReservasjon(nothing: Nothing?, id: Long) {
        TODO("Not yet implemented")
    }

    private fun nyOppgave(eksternId: UUID, event: BehandlingProsessEventDto, behandling: BehandlingK9sak): Oppgave {
        TODO("Not yet implemented")
    }

    private fun avsluttOppgaveHvisÅpen(
        behandlingId: Long,
        eksternId: UUID,
        tidligereEventer: List<OppgaveEventLogg>,
        behandlendeEnhet: String
    ) {
        TODO("Not yet implemented")
    }

    private fun avsluttOppgaveOgLoggEvent(
        eksternId: UUID,
        event: BehandlingProsessEventDto,
        eventType: OppgaveEventType,
        frist: LocalDateTime?
    ) {
        OppgaveRepository().avsluttOppgave(event.behandlingId)
        OppgaveRepository().lagre(
            OppgaveEventLogg(
                eksternId = eksternId,
                eventType = eventType,
                andreKriterierType = null,
                behandlendeEnhet = event.behandlendeEnhet,
                fristTid = frist,
                behandlingId = event.behandlingId
            )
        )
    }

    private fun loggEvent(
        behandlingId: Long,
        eksternId: UUID,
        oppgaveEventType: OppgaveEventType,
        andreKriterierType: AndreKriterierType?,
        behandlendeEnhet: String
    ) {
        OppgaveRepository().lagre(
            OppgaveEventLogg(
                eksternId = eksternId,
                eventType = oppgaveEventType,
                andreKriterierType = andreKriterierType,
                behandlendeEnhet = behandlendeEnhet,
                fristTid = null,
                behandlingId = behandlingId
            )
        )
    }

    private fun finnVentAksjonspunktFrist(aksjonspunktListe: List<Aksjonspunkt>): LocalDateTime {
        return aksjonspunktListe.filter(Aksjonspunkt::erPåVent).map(Aksjonspunkt::fristTid).first()
    }

    private fun finnManuellAksjonspunktFrist(aksjonspunktListe: List<Aksjonspunkt>): LocalDateTime {
        return aksjonspunktListe.filter(Aksjonspunkt::erManueltPåVent).map(Aksjonspunkt::fristTid).first()
    }
}