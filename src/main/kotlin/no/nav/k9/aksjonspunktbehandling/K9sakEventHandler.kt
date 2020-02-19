package no.nav.k9.aksjonspunktbehandling

import no.nav.k9.aksjonspunktbehandling.eventresultat.EventResultat.*
import no.nav.k9.domene.lager.oppgave.*
import no.nav.k9.domene.repository.BehandlingProsessEventRepository
import no.nav.k9.domene.repository.OppgaveRepository
import no.nav.k9.integrasjon.Aksjonspunkt
import no.nav.k9.integrasjon.BehandlingK9sak
import no.nav.k9.integrasjon.K9SakRestKlient
import no.nav.vedtak.felles.integrasjon.kafka.BehandlingProsessEventDto
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

class K9sakEventHandler(
    val oppgaveRepository: OppgaveRepository,
    val behandlingProsessEventRepository: BehandlingProsessEventRepository,
    val k9SakRestKlient: K9SakRestKlient
) {
    private val log = LoggerFactory.getLogger(K9sakEventHandler::class.java)

    fun prosesser(event: BehandlingProsessEventDto) {

        // event til oppgave
        // val behandling = k9SakRestKlient.getBehandling(behandlingId = event.behandlingId)
        val eventer =
            behandlingProsessEventRepository.lagreBehandlingProsessEvent(event)

//        val behandlingId = eventer.behandlingsId

//        val sisteBehandling = eventer.sisteBehandling()
//        val eksternId = sisteBehandling.uuid
//          val oppgaveEgenskapFinner = OppgaveEgenskapFinner(behandling, tidligereEventer, aksjonspunkter)

//        when (K9SakEventMapper().signifikantEventFra(eventer)) {
//            LUKK_OPPGAVE -> {
//                log.info("Lukker oppgave")
////                eventer.sisteOppgave().behandlingStatus = BehandlingStatus.AVSLUTTET
////                eventer.sisteOppgave().eventType = OppgaveEventType.LUKKET
//
//            }
//            LUKK_OPPGAVE_VENT -> {
//                log.info("Behandling satt automatisk på vent, lukker oppgave.")
////                eventer.sisteOppgave().behandlingStatus = BehandlingStatus.AVSLUTTET
////                eventer.sisteOppgave().eventType = OppgaveEventType.VENT
//            }
//            LUKK_OPPGAVE_MANUELT_VENT -> {
//                log.info("Behandling satt manuelt på vent, lukker oppgave.")
////                eventer.sisteOppgave().behandlingStatus = BehandlingStatus.AVSLUTTET
////                eventer.sisteOppgave().eventType = OppgaveEventType.MANU_VENT
//            }
//
//            GJENÅPNE_OPPGAVE -> {
//                log.info("Gjenåpner oppgave")
//                val gjenåpneOppgave = oppgaveRepository.gjenåpneOppgave(event.eksternId);
//
////                eventer.sisteOppgave().behandlingStatus = BehandlingStatus.OPPRETTET
////                eventer.sisteOppgave().eventType = OppgaveEventType.GJENAPNET
//
////                loggEvent(
////                    behandlingId = behandlingId,
////                    eksternId = gjenåpneOppgave.eksternId,
////                    oppgaveEventType = OppgaveEventType.GJENAPNET,
////                    andreKriterierType = null,
////                    behandlendeEnhet = event.behandlendeEnhet
////                )
//         //       OppgaveEgenskapHandler().håndterOppgaveEgenskaper(gjenåpneOppgave, oppgaveEgenskapFinner)
//            }
//            OPPRETT_BESLUTTER_OPPGAVE -> {
//           //     avsluttOppgaveHvisÅpen(behandlingId, eksternId, tidligereEventer, event.behandlendeEnhet)
////                val oppgave = nyOppgave(eksternId, event, behandling)
////                reserverOppgaveFraTidligereReservasjon(null)
////                loggEvent(
////                    behandlingId = behandlingId,
////                    eksternId = oppgave.eksternId,
////                    oppgaveEventType = OppgaveEventType.OPPRETTET,
////                    andreKriterierType = AndreKriterierType.TIL_BESLUTTER,
////                    behandlendeEnhet = event.behandlendeEnhet
////                )
//      //          OppgaveEgenskapHandler().håndterOppgaveEgenskaper(oppgave, oppgaveEgenskapFinner)
//            }
//            OPPRETT_PAPIRSØKNAD_OPPGAVE -> {
//        //        avsluttOppgaveHvisÅpen(behandlingId, eksternId, tidligereEventer, event.behandlendeEnhet)
////                val oppgave = nyOppgave(eksternId, event, behandling)
////                reserverOppgaveFraTidligereReservasjon(null)
////                loggEvent(
////                    behandlingId = behandlingId,
////                    eksternId = oppgave.eksternId,
////                    oppgaveEventType = OppgaveEventType.OPPRETTET,
////                    andreKriterierType = AndreKriterierType.PAPIRSØKNAD,
////                    behandlendeEnhet = event.behandlendeEnhet
////                )
//        //        OppgaveEgenskapHandler().håndterOppgaveEgenskaper(oppgave, oppgaveEgenskapFinner)
//            }
//            OPPRETT_OPPGAVE -> {
//         //       avsluttOppgaveHvisÅpen(behandlingId, eksternId, tidligereEventer, event.behandlendeEnhet)
////                val oppgave = nyOppgave(eksternId, event, behandling)
////                reserverOppgaveFraTidligereReservasjon(null)
////                loggEvent(
////                    behandlingId = behandlingId,
////                    eksternId = oppgave.eksternId,
////                    oppgaveEventType = OppgaveEventType.OPPRETTET,
////                    andreKriterierType = null,
////                    behandlendeEnhet = event.behandlendeEnhet
////                )
//            //    OppgaveEgenskapHandler().håndterOppgaveEgenskaper(oppgave, oppgaveEgenskapFinner)
//            }
//        }
    }

    private fun reserverOppgaveFraTidligereReservasjon(nothing: Nothing?) {
        TODO("Not yet implemented")
    }

    private fun nyOppgave(eksternId: UUID, event: BehandlingProsessEventDto, behandling: BehandlingK9sak): Oppgave {
        val oppgave = Oppgave(
            behandlingId = event.behandlingId,
            fagsakSaksnummer = event.saksnummer.toLong(),
            aktorId = event.aktørId.toLong(),
            behandlendeEnhet = event.behandlendeEnhet,
            behandlingType = BehandlingType.valueOf(event.behandlingTypeKode),
            fagsakYtelseType = FagsakYtelseType.valueOf(event.ytelseTypeKode),
            aktiv = true,
            forsteStonadsdag = behandling.førsteUttaksdag,
            utfortFraAdmin = false,
            behandlingsfrist = behandling.behandlingstidFrist.atStartOfDay(),
            behandlingStatus = BehandlingStatus.valueOf(behandling.status),
            eksternId = eksternId,
            behandlingOpprettet = event.opprettetBehandling,
            oppgaveAvsluttet = null,
            reservasjon = null,
            system = event.fagsystem.name
        )
        oppgaveRepository.opprettOppgave(oppgave)
        return oppgave
    }

    private fun avsluttOppgaveHvisÅpen(
        behandlingId: Long,
        eksternId: UUID,
        tidligereEventer: List<OppgaveEventLogg>,
        behandlendeEnhet: String
    ) {
        if (!tidligereEventer.isEmpty() && tidligereEventer[0].eventType.erÅpningsevent()) {
            loggEvent(
                behandlingId = behandlingId,
                eksternId = eksternId,
                oppgaveEventType = OppgaveEventType.LUKKET,
                andreKriterierType = null,
                behandlendeEnhet = behandlendeEnhet
            )
            //  oppgaveRepository.avsluttOppgave(eksternId)
        }
    }

    private fun avsluttOppgaveOgLoggEvent(
        eksternId: UUID,
        event: BehandlingProsessEventDto,
        eventType: OppgaveEventType,
        frist: LocalDateTime?
    ) {

        val oppgaveRepository = oppgaveRepository
        val oppgave = oppgaveRepository.hentOppgave(event.eksternId)
        oppgave.behandlingStatus = BehandlingStatus.AVSLUTTET
        oppgaveRepository.opprettEllerEndreOppgave(oppgave)
        oppgaveRepository.lagre(
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
        oppgaveRepository.lagre(
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