package no.nav.k9.aksjonspunktbehandling

import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.runBlocking
import no.nav.k9.Configuration
import no.nav.k9.domene.lager.oppgave.Oppgave
import no.nav.k9.domene.modell.BehandlingStatus
import no.nav.k9.domene.modell.FagsakYtelseType
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
        val oppgave = modell.oppgave()

        if (modell.fikkEndretAksjonspunkt()) {
            fjernReservasjon(oppgave)
        }
        oppgaveRepository.lagre(oppgave.eksternId) {

            if (modell.starterSak()) {
                sakOgBehadlingProducer.behandlingOpprettet(modell.behandlingOpprettetSakOgBehandling())
                if (oppgave.aktiv && oppgave.fagsakYtelseType != FagsakYtelseType.FRISINN) {
                    statistikkRepository.lagreNyHistorikk(oppgave.behandlingType.kode, oppgave.fagsakYtelseType.kode, oppgave.eksternId)
                }
            }


            if (oppgave.behandlingStatus == BehandlingStatus.AVSLUTTET) {
                fjernReservasjon(oppgave)
                if (reservasjonRepository.finnes(oppgave.eksternId)) {
                    statistikkRepository.lagreFerdigstilt(oppgave.behandlingType.kode, oppgave.eksternId)
                }
                for (oppgaveKø in oppgaveKøRepository.hent()) {
                    val set = mutableSetOf<String>()
                    oppgaveKø.nyeOgFerdigstilteOppgaver.forEach { e ->
                        e.value.forEach {
                            set.addAll(it.value.nye)
                        }
                    }
                    if (set.contains(oppgave.eksternId.toString())) {
                        runBlocking {
                            oppgaveKøRepository.lagre(oppgaveKø.id) {
                                it!!.nyeOgFerdigstilteOppgaver(oppgave)
                                    .leggTilFerdigstilt(oppgave.eksternId.toString())
                                it
                            }
                        }
                    }
                }
                statistikkRepository.lagreFerdigstiltHistorikk(oppgave.behandlingType.kode, oppgave.fagsakYtelseType.kode, oppgave.eksternId)
                sakOgBehadlingProducer.avsluttetBehandling(modell.behandlingAvsluttetSakOgBehandling())
            }

            statistikkProducer.send(modell)

            oppgave
        }
        modell.reportMetrics(reservasjonRepository)
        oppgaverSomSkalInnPåKøer.sendBlocking(oppgave)
    }


    private fun fjernReservasjon(oppgave: Oppgave) {
        if (reservasjonRepository.finnes(oppgave.eksternId)) {
            reservasjonRepository.lagre(oppgave.eksternId,true) {
                it!!.reservertTil = null

                it
            }
        }
    }

}
