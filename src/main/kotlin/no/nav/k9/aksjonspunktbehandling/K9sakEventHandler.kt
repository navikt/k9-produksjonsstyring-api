package no.nav.k9.aksjonspunktbehandling

import io.ktor.util.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.sendBlocking
import no.nav.k9.Configuration
import no.nav.k9.domene.lager.oppgave.Oppgave
import no.nav.k9.domene.modell.BehandlingStatus
import no.nav.k9.domene.modell.K9SakModell
import no.nav.k9.domene.repository.*
import no.nav.k9.integrasjon.datavarehus.StatistikkProducer
import no.nav.k9.integrasjon.kafka.dto.BehandlingProsessEventDto
import no.nav.k9.integrasjon.sakogbehandling.SakOgBehandlingProducer
import no.nav.k9.tjenester.avdelingsleder.nokkeltall.AlleOppgaverNyeOgFerdigstilte
import org.slf4j.LoggerFactory
import reportMetrics


class K9sakEventHandler @KtorExperimentalAPI constructor(
    val oppgaveRepository: OppgaveRepository,
    val behandlingProsessEventK9Repository: BehandlingProsessEventK9Repository,
    val config: Configuration,
    val sakOgBehandlingProducer: SakOgBehandlingProducer,
    val oppgaveKøRepository: OppgaveKøRepository,
    val reservasjonRepository: ReservasjonRepository,
    val statistikkProducer: StatistikkProducer,
    val oppgaverSomSkalInnPåKøer: Channel<Oppgave>,
    val statistikkRepository: StatistikkRepository,
    val saksbehhandlerRepository: SaksbehandlerRepository
) {
    private val log = LoggerFactory.getLogger(K9sakEventHandler::class.java)

    @KtorExperimentalAPI
    fun prosesser(
        event: BehandlingProsessEventDto
    ) {
        var skalSkippe = false
        val modell = behandlingProsessEventK9Repository.lagre(event.eksternId!!){
            if (it == null) {
                return@lagre  K9SakModell(mutableListOf(event))
            }
            if (it.eventer.contains(event)) {
                log.info("""Skipping eventen har kommet tidligere ${event.eventTid}""")
                skalSkippe = true
                return@lagre it
            }
            it.eventer.add(event)       
            it
        }
        if (skalSkippe) {
            return
        }
        val oppgave = modell.oppgave(modell.sisteEvent())
      
        oppgaveRepository.lagre(oppgave.eksternId) {
            if (modell.starterSak()) {
                sakOgBehandlingProducer.behandlingOpprettet(modell.behandlingOpprettetSakOgBehandling())
                if (oppgave.aktiv) {
                    statistikkRepository.lagre(
                        AlleOppgaverNyeOgFerdigstilte(
                            oppgave.fagsakYtelseType,
                            oppgave.behandlingType,
                            oppgave.eventTid.toLocalDate()
                        )
                    ) {
                        it.nye.add(oppgave.eksternId.toString())
                        it
                    }
                }
            }
            
            if (modell.forrigeEvent() != null && !modell.oppgave(modell.forrigeEvent()!!).aktiv && modell.oppgave(modell.sisteEvent()).aktiv) {
                statistikkRepository.lagre(
                    AlleOppgaverNyeOgFerdigstilte(
                        oppgave.fagsakYtelseType,
                        oppgave.behandlingType,
                        oppgave.eventTid.toLocalDate()
                    )
                ) {
                    it.nye.add(oppgave.eksternId.toString())
                    it
                }
            }

            if (modell.forrigeEvent() != null && modell.oppgave(modell.forrigeEvent()!!).aktiv && !modell.oppgave(modell.sisteEvent()).aktiv) {
                statistikkRepository.lagre(
                    AlleOppgaverNyeOgFerdigstilte(
                        oppgave.fagsakYtelseType,
                        oppgave.behandlingType,
                        oppgave.eventTid.toLocalDate()
                    )
                ) {
                    it.ferdigstilte.add(oppgave.eksternId.toString())
                    it
                }
            }

            if (oppgave.behandlingStatus == BehandlingStatus.AVSLUTTET) {
                if (reservasjonRepository.finnes(oppgave.eksternId) && reservasjonRepository.hent(oppgave.eksternId).erAktiv(event.eventTid)) {
                    statistikkRepository.lagre(
                        AlleOppgaverNyeOgFerdigstilte(
                            oppgave.fagsakYtelseType,
                            oppgave.behandlingType,
                            oppgave.eventTid.toLocalDate()
                        )
                    ) {
                        it.ferdigstilteSaksbehandler.add(oppgave.eksternId.toString())
                        it
                    }
                }
                
                if (reservasjonRepository.finnes(oppgave.eksternId)) {
                    statistikkRepository.lagreFerdigstilt(oppgave.behandlingType.kode, oppgave.eksternId)
                }
             
                sakOgBehandlingProducer.avsluttetBehandling(modell.behandlingAvsluttetSakOgBehandling())
            }

            statistikkProducer.send(modell)

            oppgave
        }
        if (modell.fikkEndretAksjonspunkt()) {
            fjernReservasjon(oppgave)
        }
        modell.reportMetrics(reservasjonRepository)
        oppgaverSomSkalInnPåKøer.sendBlocking(oppgave)
    }


    private fun fjernReservasjon(oppgave: Oppgave) {

        if (reservasjonRepository.finnes(oppgave.eksternId)) {
            reservasjonRepository.lagre(oppgave.eksternId) { reservasjon ->
                reservasjon!!.reservertTil = null
                reservasjon
            }
            val reservasjon = reservasjonRepository.hent(oppgave.eksternId)
            saksbehhandlerRepository.fjernReservasjonIkkeTaHensyn(
                reservasjon.reservertAv,
                reservasjon.oppgave
            )
        }
    }

}
