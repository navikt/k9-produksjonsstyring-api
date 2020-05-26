
import io.prometheus.client.Counter
import io.prometheus.client.Histogram
import no.nav.k9.domene.modell.BehandlingStatus
import no.nav.k9.domene.modell.Modell
import java.time.Duration

private val oppgaveOpprettet = Counter.build()
    .name("oppgaveOpprettet_counter")
    .help("Teller for antall opprettede oppgaver")
    .register()

private val oppgaveAvsluttet = Counter.build()
    .name("oppgaveAvsluttet_counter")
    .help("Teller for antall avsluttede oppgaver")
    .register()

private val ledetid = Histogram.build()
    .name("ledetid_behandling_av_oppgave")
    .help("Ledetid behandling av oppgaver")
    .register()

internal fun Modell.reportMetrics() {
    val oppgave = oppgave()
    if (starterSak()) {
        oppgaveOpprettet.inc()    
    }
    
    if (oppgave.behandlingStatus == BehandlingStatus.AVSLUTTET) {
        oppgaveAvsluttet.inc()
        val between = Duration.between(førsteEvent().eventTid, sisteEvent().eventTid)
        ledetid.observe(between.toDays().toDouble())
    }
}