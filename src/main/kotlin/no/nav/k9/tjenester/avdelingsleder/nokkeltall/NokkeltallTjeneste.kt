package no.nav.k9.tjenester.avdelingsleder.nokkeltall

import io.ktor.util.KtorExperimentalAPI
import no.nav.k9.domene.repository.OppgaveRepository
import no.nav.k9.domene.repository.StatistikkRepository
import no.nav.k9.tjenester.saksbehandler.oppgave.OppgaveTjeneste
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*

private val log: Logger =
    LoggerFactory.getLogger(NokkeltallTjeneste::class.java)

class NokkeltallTjeneste @KtorExperimentalAPI constructor(
    private val oppgaveRepository: OppgaveRepository,
    private val statistikkRepository: StatistikkRepository
) {

    fun hentOppgaverUnderArbeid(): List<AlleOppgaverDto> {
        return oppgaveRepository.hentAlleOppgaverUnderArbeid()
    }

    fun hentNyeFerdigstilteOppgaverOppsummering(): List<AlleOppgaverNyeOgFerdigstilteDto> {
        val ferdigstilteManuelt = statistikkRepository.hentFerdigstilte()
        return statistikkRepository.hentFerdigstilteOgNyeHistorikkPerAntallDager(7).map {
            val ferdigstilte =
                    ferdigstilteManuelt.find { f -> f.behandlingType == it.behandlingType && f.dato == it.dato }
            AlleOppgaverNyeOgFerdigstilteDto(
                    it.fagsakYtelseType,
                    it.behandlingType,
                    it.dato,
                    it.nye.size,
                    ferdigstilte?.antall ?: it.ferdigstilte.size,
                    )
        }
    }

    fun hentFerdigstilteSiste8Uker(): List<AlleOppgaverHistorikk> {
        return statistikkRepository.hentFerdigstilteOgNyeHistorikkSiste8Uker().map {
            AlleOppgaverHistorikk(
                    it.fagsakYtelseType,
                    it.behandlingType,
                    it.dato,
                    it.ferdigstilte.size
            )
        }
    }

    fun hentNyeSiste8Uker(): List<AlleOppgaverHistorikk> {
        return statistikkRepository.hentFerdigstilteOgNyeHistorikkSiste8Uker().map {
            AlleOppgaverHistorikk(
                    it.fagsakYtelseType,
                    it.behandlingType,
                    it.dato,
                    it.nye.size
            )
        }
    }

    fun hentDagensTall(): List<AlleApneBehandlinger> {
        return oppgaveRepository.hentApneBehandlingerPerBehandlingtypeIdag()
    }
}
