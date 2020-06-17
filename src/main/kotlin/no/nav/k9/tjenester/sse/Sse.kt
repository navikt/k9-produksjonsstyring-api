package no.nav.k9.tjenester.sse

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.CacheControl
import io.ktor.http.ContentType
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.response.cacheControl
import io.ktor.response.respondText
import io.ktor.response.respondTextWriter
import io.ktor.routing.Route
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.ReceiveChannel
import org.slf4j.LoggerFactory

val log = LoggerFactory.getLogger("Route.Sse")

@ExperimentalCoroutinesApi
@KtorExperimentalAPI
@KtorExperimentalLocationsAPI
internal fun Route.Sse(
    sseChannel: BroadcastChannel<SseEvent>
) {
    @Location("/sse")
    class sse
    get { _: sse ->
        val events = sseChannel.openSubscription()
        try {
            call.respondSse(events)
        } finally {
            events.cancel()
        }
    }

    @Location("/sse2")
    class sse2
    get { _: sse2 ->
        call.respondText(
            """
                        <html>
                            <head></head>
                            <body>
                                <ul id="events">
                                </ul>
                                <script type="text/javascript">
                                    var source = new EventSource('/api/sse');
                                    var eventsUl = document.getElementById('events');
                                    function logEvent(text) {
                                        var li = document.createElement('li')
                                        li.innerText = text;
                                        eventsUl.appendChild(li);
                                    }
                                    source.addEventListener('message', function(e) {
                                        logEvent('message:' + e.data);
                                    }, false);
                                    source.addEventListener('open', function(e) {
                                        logEvent('open');
                                    }, false);
                                    source.addEventListener('error', function(e) {
                                        if (e.readyState == EventSource.CLOSED) {
                                            logEvent('closed');
                                        } else {
                                            logEvent('error');
                                            console.log(e);
                                        }
                                    }, false);
                                </script>
                            </body>
                        </html>
                    """.trimIndent(),
            contentType = ContentType.Text.Html
        )
    }

}

suspend fun ApplicationCall.respondSse(events: ReceiveChannel<SseEvent>) {
    response.cacheControl(CacheControl.NoCache(null))
    respondTextWriter(contentType = ContentType.Text.EventStream) {
        write("data: open\n")
        write("\n")
        flush()
        for (event in events) {
            for (dataLine in event.data.lines()) {
                write("data: $dataLine\n")
                log.info("data: $dataLine")
            }
            write("\n")
            flush()
        }
    }
}
