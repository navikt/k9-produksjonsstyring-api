package no.nav.k9.tjenester.sse

import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import no.nav.k9.tjenester.sse.RefreshKlienter.sseOperation
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("no.nav.RefreshKlienterWebSocket")

internal fun Route.RefreshKlienterWebSocket(sseChannel: BroadcastChannel<SseEvent>) {
    webSocket("/ws") {
        logger.info("WebSocket opened.")
        val events = sseOperation("openSubscription") {
            sseChannel.openSubscription()
        }

        try {
            for (event in events) {
                for (dataLine in event.data.lines()) {
                    outgoing.send(Frame.Text(dataLine) as Frame)
                }
            }
        } catch (closed: ClosedReceiveChannelException) {
            events.cancel()
            logger.info("WebSocket closed.")
        } catch (throwable: Throwable) {
            events.cancel()
            logger.error("WebSocket error.", throwable)
        }
    }
}
