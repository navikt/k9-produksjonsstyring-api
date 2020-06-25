package no.nav.k9.tjenester.avdelingsleder.nokkeltall

import io.ktor.util.KtorExperimentalAPI
import no.nav.k9.domene.repository.OppgaveRepository
import no.nav.k9.domene.repository.StatistikkRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val log: Logger =
    LoggerFactory.getLogger(NokkeltallTjeneste::class.java)

class NokkeltallTjeneste @KtorExperimentalAPI constructor(
    private val oppgaveRepository: OppgaveRepository,
    private val statistikkRepository: StatistikkRepository
) {

    fun hentOppgaverUnderArbeid(): List<AlleOppgaverDto> {
        return oppgaveRepository.hentAlleOppgaverUnderArbeid()
    }

    fun hentOppgaverPerDato(): List<AlleOppgaverPerDato> {
        return oppgaveRepository.hentAlleOppgaverPerDato()
    }


    fun hentFerdigstilteOppgaver(): List<AlleFerdigstilteOppgaverDto> {
        return statistikkRepository.hentFerdigstilte().groupBy { it.behandlingType }.entries.map { entry ->
            AlleFerdigstilteOppgaverDto(
                entry.key,
                entry.value.maxBy { it.dato }!!.antall,
                entry.value.sumBy { it.antall })
        }
    }
}
