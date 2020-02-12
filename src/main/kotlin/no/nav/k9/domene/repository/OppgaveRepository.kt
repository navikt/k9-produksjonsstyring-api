package no.nav.k9.domene.repository

import no.nav.k9.domene.lager.oppgave.Oppgave
import no.nav.k9.domene.lager.oppgave.OppgaveEventLogg
import java.util.*

class OppgaveRepository () {
    fun hentEventer(uuid: UUID): List<OppgaveEventLogg> {
        TODO("Not yet implemented")

    }

    fun avsluttOppgave(behandlingId: Long) {
        TODO("Not yet implemented")
    }

    fun lagre(oppgaveEventLogg: OppgaveEventLogg) {
        TODO("Not yet implemented")
    }

    fun gjenåpneOppgave(eksternId: UUID): Oppgave {
        TODO("Not yet implemented")
    }
}