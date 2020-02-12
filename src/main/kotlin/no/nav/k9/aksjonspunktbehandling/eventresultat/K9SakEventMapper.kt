package no.nav.k9.aksjonspunktbehandling.eventresultat

import no.nav.k9.domene.lager.oppgave.OppgaveEventLogg
import no.nav.k9.integrasjon.Aksjonspunkt

class K9SakEventMapper (){
    fun signifikantEventFra(
        aksjonspunkter: List<Aksjonspunkt>,
        tidligereEventer: List<OppgaveEventLogg>,
        behandlendeEnhet: String
    ): EventResultat {
        TODO("Not yet implemented")
    }

}