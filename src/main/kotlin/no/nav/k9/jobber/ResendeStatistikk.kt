package no.nav.k9.jobber

import io.ktor.util.*
import no.nav.k9.domene.repository.BehandlingProsessEventRepository
import no.nav.k9.integrasjon.datavarehus.StatistikkProducer
import org.slf4j.LoggerFactory
import java.text.DecimalFormat
import java.util.*
import kotlin.system.measureTimeMillis


class ResendeStatistikk(
    private val behandlingProsessEventRepository: BehandlingProsessEventRepository,
    private val statistikkProducer: StatistikkProducer
) {
    private val log = LoggerFactory.getLogger(ResendeStatistikk::class.java)

    @KtorExperimentalAPI
    fun resend() {
        
        val df = DecimalFormat("##.##%")
        val hentAlleEventerIder = behandlingProsessEventRepository.hentAlleEventerIder()
        log.info("Starter sending av statistikk til k9-statistikk, ${hentAlleEventerIder.size} eventer")
        val totalTid = measureTimeMillis {
            for ((index, uuid) in hentAlleEventerIder.withIndex()) {
                if (index % 1000 == 0) {
                    log.info("Statistikk, ferdig med ${df.format((index.toDouble() / hentAlleEventerIder.size.toDouble()))}")
                }
                for (modell in behandlingProsessEventRepository.hent(UUID.fromString(uuid)).alleVersjoner()) {
                    statistikkProducer.send(modell)
                }
            }
        }
        log.info("Resending av all statistikk til k9-statistikk tok $totalTid")
    }
}
