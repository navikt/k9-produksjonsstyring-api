package no.nav.k9.aksjonspunktbehandling

import no.nav.k9.domene.lager.oppgave.OppgaveEventLogg
import no.nav.k9.integrasjon.Aksjonspunkt
import no.nav.k9.integrasjon.BehandlingK9sak

class OppgaveEgenskapFinner(
    behandling: BehandlingK9sak,
    tidligereEventer: List<OppgaveEventLogg>,
    aksjonspunkter: List<Aksjonspunkt>
) {}