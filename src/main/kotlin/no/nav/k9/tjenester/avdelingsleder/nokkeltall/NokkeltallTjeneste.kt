package no.nav.k9.tjenester.avdelingsleder.nokkeltall

import io.ktor.util.KtorExperimentalAPI
import no.nav.k9.domene.repository.OppgaveRepository
import no.nav.k9.tjenester.saksbehandler.oppgave.OppgaveTjeneste
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val log: Logger =
    LoggerFactory.getLogger(NokkeltallTjeneste::class.java)

class NokkeltallTjeneste @KtorExperimentalAPI constructor(
    oppgaveRepository: OppgaveRepository
) {

    fun hentOppgaverUnderArbeid(): List<AlleOppgaverDto> {
        return emptyList()
    }
}
