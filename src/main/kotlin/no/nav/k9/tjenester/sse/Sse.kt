package no.nav.k9.tjenester.sse

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.CacheControl
import io.ktor.http.ContentType
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.response.*
import io.ktor.routing.Route
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.ReceiveChannel
import no.nav.k9.tjenester.sse.RefreshKlienter.sseOperation
import no.nav.k9.tjenester.sse.RefreshKlienter.sseOperationCo
import org.slf4j.LoggerFactory

val log = LoggerFactory.getLogger("Route.Sse")

// https://github.com/ktorio/ktor-samples/blob/1.3.0/other/sse/src/SseApplication.kt
@ExperimentalCoroutinesApi
@KtorExperimentalAPI
@KtorExperimentalLocationsAPI
internal fun Route.Sse(
    sseChannel: BroadcastChannel<SseEvent>) {

    @Location("/sse")
    class sse
    get { _: sse ->
        val events = sseOperation("openSubscription") {
            sseChannel.openSubscription()
        }
        try {
            sseOperationCo("respondSse") {
                call.respondSse(events)
            }
        } finally {
            events.cancel()
        }
    }
}

@ExperimentalCoroutinesApi
suspend fun ApplicationCall.respondSse(events: ReceiveChannel<SseEvent>) {
    response.cacheControl(CacheControl.NoCache(null))

    respondTextWriter(contentType = ContentType.Text.EventStream) {
        for (event in events) {
            for (dataLine in event.data.lines()) {
                write("data: $dataLine\n")
            }
            write("\n")
            flush()
        }
    }
}