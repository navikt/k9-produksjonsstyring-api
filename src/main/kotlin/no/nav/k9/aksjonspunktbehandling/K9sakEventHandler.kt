package no.nav.k9.aksjonspunktbehandling

import io.ktor.util.KtorExperimentalAPI
import no.nav.k9.Configuration
import no.nav.k9.domene.lager.oppgave.Oppgave
import no.nav.k9.domene.modell.BehandlingStatus
import no.nav.k9.domene.repository.BehandlingProsessEventRepository
import no.nav.k9.domene.repository.OppgaveKøRepository
import no.nav.k9.domene.repository.OppgaveRepository
import no.nav.k9.domene.repository.ReservasjonRepository
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
    val statistikkProducer: StatistikkProducer
) {
    private val log = LoggerFactory.getLogger(K9sakEventHandler::class.java)

    @KtorExperimentalAPI
    fun prosesser(
        event: BehandlingProsessEventDto
    ) {
        val modell = behandlingProsessEventRepository.lagre(event)

        val oppgave = modell.oppgave()

        oppgaveRepository.lagre(oppgave.eksternId) {
            if (modell.starterSak()) {
                sakOgBehadlingProducer.behandlingOpprettet(modell.behandlingOpprettet(modell))
            }

            if (oppgave.behandlingStatus == BehandlingStatus.AVSLUTTET) {
                sakOgBehadlingProducer.avsluttetBehandling(modell.behandlingAvsluttet(modell))
            }

            statistikkProducer.send(modell)
            
            oppgave
        }
        modell.reportMetrics(reservasjonRepository)
        oppdaterOppgavekøer(oppgave)
    }

    private fun oppdaterOppgavekøer(oppgave: Oppgave) {
        fjernReservasjonDersomIkkeOppgavenErAktiv(oppgave)
        for (oppgavekø in oppgaveKøRepository.hent()) {
            oppgaveKøRepository.lagre(oppgavekø.id) { o ->
                o?.leggOppgaveTilEllerFjernFraKø(oppgave, reservasjonRepository)
                o!!
            }
        }
    }
    
    private fun fjernReservasjonDersomIkkeOppgavenErAktiv(oppgave: Oppgave) {
        if (!oppgave.aktiv) {
            if (reservasjonRepository.finnes(oppgave.eksternId)) {
                reservasjonRepository.lagre(oppgave.eksternId) { reservasjon ->
                    reservasjon!!.reservertTil = null
                    reservasjon
                }
            }
        }
    }
}