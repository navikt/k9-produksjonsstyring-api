
import io.prometheus.client.Counter
import no.nav.k9.domene.modell.BehandlingStatus
import no.nav.k9.domene.modell.Modell

private val oppgaveOpprettet = Counter.build()
    .name("oppgaveOpprettet_counter")
    .help("Teller for antall opprettede oppgaver")
    .register()

private val oppgaveAvsluttet = Counter.build()
    .name("oppgaveAvsluttet_counter")
    .help("Teller for antall avsluttede oppgaver")
    .register()

internal fun Modell.reportMetrics() {
    val oppgave = oppgave()
    if (starterSak()) {
        oppgaveOpprettet.inc()    
    }
    
    if (oppgave.behandlingStatus == BehandlingStatus.AVSLUTTET) {
        oppgaveAvsluttet.inc()
    }
}