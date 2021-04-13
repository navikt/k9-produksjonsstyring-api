package no.nav.k9.websocket

import io.ktor.application.*
import io.ktor.client.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.locations.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.testing.*
import io.ktor.util.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.consumeEach
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.httpGet
import no.nav.k9.apis.ServerSentEventsTest

import no.nav.k9.tjenester.sse.Melding
import no.nav.k9.tjenester.sse.RefreshKlienter
import no.nav.k9.tjenester.sse.RefreshKlienter.sendMelding
import no.nav.k9.tjenester.sse.RefreshKlienter.sseChannel
import no.nav.k9.tjenester.sse.Sse
import no.nav.k9.tjenester.sse.SseEvent
import org.json.JSONObject
import org.junit.Test
import java.net.HttpURLConnection
import java.net.URL
import java.time.Duration

import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@KtorExperimentalAPI
@ExperimentalCoroutinesApi
class WebSocketTest {
    @Test
    fun testConversation() {
        withTestApplication {
            application.install(WebSockets)

            val received = arrayListOf<String>()
            application.routing {
                webSocket("/refresh") {
                    try {
                        while (true) {
                            val text = (incoming.receive() as Frame.Text).readText()
                            received += text
                            outgoing.send(Frame.Text(text))
                        }
                    } catch (e: ClosedReceiveChannelException) {
                        // Do nothing!
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                }
            }

            handleWebSocketConversation("/refresh") { incoming, outgoing ->
                val textMessages = listOf("data: {\"melding\":\"oppdaterTilBehandling\",\"id\":\"1688b174-184c-4aee-a56b-3c7c30fa5fa0\"}", "data: {\"melding\":\"oppdaterReserverte\",\"id\":\"1688b174-184c-4aee-a56b-3c7c30fa5fa0\"}")
                for (msg in textMessages) {
                    outgoing.send(Frame.Text(msg))
                    assertEquals(msg, (incoming.receive() as Frame.Text).readText())
                }
                assertEquals(textMessages, received)
            }
        }
    }

    private fun Application.websocketTestApp(refreshKlienter: Channel<SseEvent>) {

        install(WebSockets)
        install(Locations)

        val received = arrayListOf<String>()
        routing {
            webSocket("/ws") {
                try {
                    while (true) {
                        val text = (incoming.receive() as Frame.Text).readText()
                        received += text
                        outgoing.send(Frame.Text(text))
                    }
                } catch (e: ClosedReceiveChannelException) {
                    // Do nothing!
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
            routing {
                get("/isready") {
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
}


    private fun withNettyEngine(refreshKlienter: Channel<SseEvent>, block: suspend () -> Unit) {
        val server = embeddedServer(Netty, applicationEngineEnvironment {
            module { websocketTestApp(refreshKlienter) }
            connector { port = 1234 }
        })
        val job = GlobalScope.launch {
            server.start(wait = true)
        }

        runBlocking {
            for (i in 1..20) {
                delay(i * 1000L)
                if ("http://localhost:1234/isready".httpGet().second.isSuccess) {
                    break
                }
            }
        }

        try {
            runBlocking { block() }
        } finally {
            server.stop(1000,1000)
            runBlocking { job.cancelAndJoin() }
        }
    }
}

