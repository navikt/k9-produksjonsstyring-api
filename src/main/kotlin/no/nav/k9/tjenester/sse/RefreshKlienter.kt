package no.nav.k9.tjenester.sse

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.application.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.broadcast
import kotlinx.coroutines.channels.produce
import no.nav.k9.aksjonspunktbehandling.objectMapper
import java.util.*

@ExperimentalCoroutinesApi
internal object RefreshKlienter {
    private val objectMapper = objectMapper().also {
        it.disable(SerializationFeature.INDENT_OUTPUT)
    }
    private inline fun <reified T> ObjectMapper.asString(value: T): String = writeValueAsString(value)


    internal fun Application.sseChannel(channel: Channel<SseEvent>) = produce {
        for (oppgaverOppdatertEvent in channel) {
            send(oppgaverOppdatertEvent)
        }
    }.broadcast()

    internal fun initializeRefreshKlienter() = Channel<SseEvent>(Channel.UNLIMITED)

    internal suspend fun Channel<SseEvent>.sendMelding(melding: Melding) {
        val event = SseEvent(data = objectMapper.asString(melding))
        send(event)
    }

    internal suspend fun Channel<SseEvent>.sendOppdaterTilBehandling(uuid: UUID) = sendMelding(oppdaterTilBehandlingMelding(uuid))

    internal suspend fun Channel<SseEvent>.sendOppdaterReserverte() = sendMelding(oppdaterReserverteMelding())

    internal fun oppdaterTilBehandlingMelding(uuid: UUID) = Melding(
        melding = "oppdaterTilBehandling",
        id = "$uuid"
    )

    internal fun oppdaterReserverteMelding() = Melding(
        melding = "oppdaterReserverte"
    )
}