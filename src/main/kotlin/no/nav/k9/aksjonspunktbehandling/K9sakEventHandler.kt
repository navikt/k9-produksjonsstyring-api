package no.nav.k9.aksjonspunktbehandling

import io.ktor.util.KtorExperimentalAPI
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
    val saksbehandlerRepository: SaksbehandlerRepository,
    val statistikkProducer: StatistikkProducer
//    val gosysOppgaveGateway: GosysOppgaveGateway
) {
    private val log = LoggerFactory.getLogger(K9sakEventHandler::class.java)

    @KtorExperimentalAPI
    fun prosesser(
        event: BehandlingProsessEventDto
    ) {
        val modell = behandlingProsessEventRepository.lagre(event)
        
        val oppgave = modell.oppgave()
        
        fjernReservasjonDersomIkkeOppgavenErAktiv(oppgave)
     
        oppgaveRepository.lagre(oppgave.eksternId) {
            if (!config.erLokalt()) {
                if (modell.starterSak()) {
                    sakOgBehadlingProducer.behandlingOpprettet(modell.behandlingOpprettet(modell))
                }

                if (oppgave.behandlingStatus == BehandlingStatus.AVSLUTTET) {
                    sakOgBehadlingProducer.avsluttetBehandling(modell.behandlingAvsluttet(modell))
                }

                if (config.erIDevFss) {
                    statistikkProducer.sendSak(modell.dvhSak())
                    statistikkProducer.sendBehandling(
                        modell.dvhBehandling(
                            saksbehandlerRepository = saksbehandlerRepository,
                            reservasjonRepository = reservasjonRepository
                        )
                    )
                }
            }
            oppgave
        }
        modell.reportMetrics(reservasjonRepository)
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
                    reservasjon!!.aktiv = false
                    reservasjon
                }
            }
        }
    }
}