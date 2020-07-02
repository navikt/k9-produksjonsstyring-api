package no.nav.k9.aksjonspunktbehandling

import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import no.nav.k9.Configuration
import no.nav.k9.domene.lager.oppgave.Oppgave
import no.nav.k9.domene.modell.BehandlingStatus
import no.nav.k9.domene.repository.*
import no.nav.k9.integrasjon.datavarehus.StatistikkProducer
import no.nav.k9.integrasjon.kafka.dto.BehandlingProsessEventDto
import no.nav.k9.integrasjon.sakogbehandling.SakOgBehadlingProducer
import org.slf4j.LoggerFactory
import reportMetrics


class K9sakEventHandler @KtorExperimentalAPI constructor(
    val oppgaveRepository: OppgaveRepository,
    val behandlingProsessEventRepository: BehandlingProsessEventRepository,
    val config: Configuration,
    val sakOgBehadlingProducer: SakOgBehadlingProducer,
    val oppgaveKøRepository: OppgaveKøRepository,
    val reservasjonRepository: ReservasjonRepository,
    val statistikkProducer: StatistikkProducer,
    val oppgaverSomSkalInnPåKøer: Channel<Oppgave>,
    val statistikkRepository: StatistikkRepository
) {
    private val log = LoggerFactory.getLogger(K9sakEventHandler::class.java)

    @KtorExperimentalAPI
    fun prosesser(
        event: BehandlingProsessEventDto
    ) {
        val modell = behandlingProsessEventRepository.lagre(event)
        // log.info(objectMapper().writeValueAsString(event))
        val oppgave = modell.oppgave()

        // fjernReservasjon(oppgave)
        if (modell.fikkEndretAksjonspunkt()) {
            fjernReservasjon(oppgave)
        }
        oppgaveRepository.lagre(oppgave.eksternId) {

            if (modell.starterSak()) {
                sakOgBehadlingProducer.behandlingOpprettet(modell.behandlingOpprettetSakOgBehandling())
            }

            if (oppgave.behandlingStatus == BehandlingStatus.AVSLUTTET) {
                fjernReservasjon(oppgave)
                if (reservasjonRepository.finnes(oppgave.eksternId)) {
                    log.info("En ny ferdigstilt oppgave " + oppgave.fagsakSaksnummer)
                    statistikkRepository.lagreFerdigstilt(oppgave.behandlingType.kode, oppgave.eksternId)
                    oppgaveKøRepository.hent().forEach {kø ->
                        if (kø.tilhørerOppgaveTilKø(oppgave, reservasjonRepository)) {
                            log.info("Legger ferdigstilt oppgave " + oppgave.eksternId.toString() + " til køen " + kø.navn)
                            kø.nyeOgFerdigstilteOppgaverDto(oppgave).leggTilFerdigstilt(oppgave.eksternId.toString())
                        }
                    }
                }
                sakOgBehadlingProducer.avsluttetBehandling(modell.behandlingAvsluttetSakOgBehandling())
            }

            statistikkProducer.send(modell)

            oppgave
        }
        modell.reportMetrics(reservasjonRepository)
        runBlocking {
            oppgaverSomSkalInnPåKøer.send(oppgave)
        }
    }

    private fun fjernReservasjon(oppgave: Oppgave) {
        if (reservasjonRepository.finnes(oppgave.eksternId)) {
            reservasjonRepository.lagre(oppgave.eksternId) {
                it!!.reservertTil = null
                it
            }
        }
    }

}
