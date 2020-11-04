package no.nav.k9.aksjonspunktbehandling

import io.ktor.util.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.sendBlocking
import no.nav.k9.Configuration
import no.nav.k9.domene.lager.oppgave.Oppgave
import no.nav.k9.domene.modell.BehandlingStatus
import no.nav.k9.domene.repository.*
import no.nav.k9.integrasjon.datavarehus.StatistikkProducer
import no.nav.k9.integrasjon.kafka.dto.BehandlingProsessEventTilbakeDto
import no.nav.k9.integrasjon.sakogbehandling.SakOgBehandlingProducer
import no.nav.k9.tjenester.avdelingsleder.nokkeltall.AlleOppgaverNyeOgFerdigstilte
import org.slf4j.LoggerFactory


class K9TilbakeEventHandler @KtorExperimentalAPI constructor(
    val oppgaveRepository: OppgaveRepository,
    val behandlingProsessEventTilbakeRepository: BehandlingProsessEventTilbakeRepository,
    val config: Configuration,
    val sakOgBehandlingProducer: SakOgBehandlingProducer,
    val oppgaveKøRepository: OppgaveKøRepository,
    val reservasjonRepository: ReservasjonRepository,
    val statistikkProducer: StatistikkProducer,
    val oppgaverSomSkalInnPåKøer: Channel<Oppgave>,
    val statistikkRepository: StatistikkRepository,
    val saksbehhandlerRepository: SaksbehandlerRepository
) {
    private val log = LoggerFactory.getLogger(K9TilbakeEventHandler::class.java)

    @KtorExperimentalAPI
    fun prosesser(
        event: BehandlingProsessEventTilbakeDto
    ) {
        val modell = behandlingProsessEventTilbakeRepository.lagre(event)
        val oppgave = modell.oppgave(modell.sisteEvent())

        if (modell.fikkEndretAksjonspunkt()) {
            fjernReservasjon(oppgave)
        }
        oppgaveRepository.lagre(oppgave.eksternId) {
            if (modell.starterSak()) {
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
                fjernReservasjon(oppgave)
                if (reservasjonRepository.finnes(oppgave.eksternId)) {
                    statistikkRepository.lagreFerdigstilt(oppgave.behandlingType.kode, oppgave.eksternId, oppgave.eventTid.toLocalDate())
                }

                sakOgBehandlingProducer.avsluttetBehandling(modell.behandlingAvsluttetSakOgBehandling())
            }

            oppgave
        }
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
