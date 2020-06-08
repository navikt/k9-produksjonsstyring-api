package no.nav.k9.aksjonspunktbehandling

import io.ktor.util.KtorExperimentalAPI
import no.nav.k9.Configuration
import no.nav.k9.domene.repository.BehandlingProsessEventRepository
import no.nav.k9.domene.repository.OppgaveKøRepository
import no.nav.k9.domene.repository.OppgaveRepository
import no.nav.k9.domene.repository.ReservasjonRepository
import no.nav.k9.integrasjon.datavarehus.StatistikkProducer
import no.nav.k9.integrasjon.sakogbehandling.SakOgBehadlingProducer
import org.slf4j.LoggerFactory
import java.util.*


class ResendStatistikk @KtorExperimentalAPI constructor(
    val oppgaveRepository: OppgaveRepository,
    val behandlingProsessEventRepository: BehandlingProsessEventRepository,
    val config: Configuration,
    val sakOgBehadlingProducer: SakOgBehadlingProducer,
    val oppgaveKøRepository: OppgaveKøRepository,
    val reservasjonRepository: ReservasjonRepository,
    val statistikkProducer: StatistikkProducer
) {
    private val log = LoggerFactory.getLogger(ResendStatistikk::class.java)

    @KtorExperimentalAPI
    fun prosesser(
       
    ) {
        for (eventId in behandlingProsessEventRepository.hentAlleEventerIder()) {
            for (modell in behandlingProsessEventRepository.hent(UUID.fromString(eventId)).alleVersjoner()) {
             statistikkProducer.send(modell)
            }
        }
    }
}