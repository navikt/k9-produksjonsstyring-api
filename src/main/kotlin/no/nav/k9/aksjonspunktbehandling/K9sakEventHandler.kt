package no.nav.k9.aksjonspunktbehandling

import io.ktor.util.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.runBlocking
import no.nav.k9.Configuration
import no.nav.k9.domene.lager.oppgave.Oppgave
import no.nav.k9.domene.modell.BehandlingStatus
import no.nav.k9.domene.repository.*
import no.nav.k9.integrasjon.datavarehus.StatistikkProducer
import no.nav.k9.integrasjon.kafka.dto.BehandlingProsessEventDto
import no.nav.k9.integrasjon.sakogbehandling.SakOgBehandlingProducer
import no.nav.k9.tjenester.avdelingsleder.nokkeltall.AlleOppgaverNyeOgFerdigstilte
import org.slf4j.LoggerFactory
import reportMetrics


class K9sakEventHandler @KtorExperimentalAPI constructor(
    val oppgaveRepository: OppgaveRepository,
    val behandlingProsessEventRepository: BehandlingProsessEventRepository,
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
        val modell = behandlingProsessEventRepository.lagre(event)
        val oppgave = modell.oppgave()

        if (modell.fikkEndretAksjonspunkt()) {
            fjernReservasjon(oppgave)
        }
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
            
            if (modell.forrigeEvent() != null && !modell.oppgave(modell.forrigeEvent()!!).aktiv && modell.oppgave().aktiv) {
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

            if (modell.forrigeEvent() != null && modell.oppgave(modell.forrigeEvent()!!).aktiv && !modell.oppgave().aktiv) {
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
                fjernReservasjon(oppgave)
                if (reservasjonRepository.finnes(oppgave.eksternId)) {
                    statistikkRepository.lagreFerdigstilt(oppgave.behandlingType.kode, oppgave.eksternId)
                }
                for (oppgaveKø in oppgaveKøRepository.hentIkkeTaHensyn()) {
                    val set = mutableSetOf<String>()
                    oppgaveKø.nyeOgFerdigstilteOppgaver.forEach { e ->
                        e.value.forEach {
                            set.addAll(it.value.nye)
                        }
                    }
                    if (set.contains(oppgave.eksternId.toString())) {
                        runBlocking {
                            oppgaveKøRepository.lagreIkkeTaHensyn(oppgaveKø.id) {
                                it!!.nyeOgFerdigstilteOppgaver(oppgave)
                                    .leggTilFerdigstilt(oppgave.eksternId.toString())
                                it
                            }
                        }
                    }
                }
                sakOgBehandlingProducer.avsluttetBehandling(modell.behandlingAvsluttetSakOgBehandling())
            }

            statistikkProducer.send(modell)

            oppgave
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
