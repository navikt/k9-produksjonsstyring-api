package no.nav.k9.eventhandler

import io.ktor.util.*
import kotlinx.coroutines.runBlocking
import no.nav.k9.domene.repository.ReservasjonRepository
import no.nav.k9.domene.repository.SaksbehandlerRepository
import java.util.*
import kotlin.concurrent.fixedRateTimer

@KtorExperimentalAPI
fun sjekkReserverteJobb(
    reservasjonRepository: ReservasjonRepository,
    saksbehandlerRepository: SaksbehandlerRepository
): Timer {
    return fixedRateTimer(
        name = "sjekkReserverteTimer", daemon = true,
        initialDelay = 0, period = 900 * 1000
    ) {
        val reservasjoner = saksbehandlerRepository.hentAlleSaksbehandlereIkkeTaHensyn().flatMap { it.reservasjoner }
        runBlocking { reservasjonRepository.hent(reservasjoner.toSet()) }
    }
}