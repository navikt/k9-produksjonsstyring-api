package no.nav.k9.apis

import assertk.assertThat
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.locations.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.httpGet
import no.nav.k9.tjenester.sse.Melding
import no.nav.k9.tjenester.sse.RefreshKlienter.initializeRefreshKlienter
import no.nav.k9.tjenester.sse.RefreshKlienter.oppdaterReserverteMelding
import no.nav.k9.tjenester.sse.RefreshKlienter.oppdaterTilBehandlingMelding
import no.nav.k9.tjenester.sse.RefreshKlienter.sendMelding
import no.nav.k9.tjenester.sse.RefreshKlienter.sseChannel
import no.nav.k9.tjenester.sse.Sse
import no.nav.k9.tjenester.sse.SseEvent
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import kotlin.test.Test
import kotlin.test.assertTrue

@KtorExperimentalAPI
@ExperimentalCoroutinesApi
class ServerSentEventsTest {

    @Test
    fun `sende og motta server sent events`() {
        val refreshKlienter = initializeRefreshKlienter()

        withNettyEngine(refreshKlienter) {
            val meldinger = genererRandomMeldinger()

            GlobalScope.launch {
                for (melding in meldinger) {
                    refreshKlienter.sendMelding(melding)
                    delay(50L)
                }
            }

            val mottatteMeldinger = hentMeldinger()

            assertTrue(mottatteMeldinger.containsAll(meldinger))
        }
    }


    private companion object {
        private const val AntallMeldinger = 100

        private fun genererRandomMeldinger() = (1..AntallMeldinger).map { when ((0..1).random()) {
            0 -> oppdaterTilBehandlingMelding(UUID.randomUUID())
            else -> oppdaterReserverteMelding()
        }}

        // https://medium.com/@manoel.al.amaro/understand-kotlin-flow-coroutines-by-implementing-server-side-sent-sse-9e190ff5f24f
        private fun hentMeldinger(): List<Melding> {
            val conn = (URL("http://localhost:1234/sse").openConnection() as HttpURLConnection).also {
                it.setRequestProperty("Accept", "text/event-stream")
                it.doInput = true
            }


            conn.connect()

            val inputReader = conn.inputStream.bufferedReader()

            val mottatteMeldinger = mutableListOf<Melding>()

            while (mottatteMeldinger.size < AntallMeldinger) {
                inputReader.readLine()?.takeIf { it.isNotBlank() }?.also {
                    val json = JSONObject(it.removePrefix("data: "))
                    when (json.has("id") && !json.isNull("id")) {
                        true -> mottatteMeldinger.add(oppdaterTilBehandlingMelding(UUID.fromString(json.getString("id"))))
                        false -> mottatteMeldinger.add(oppdaterReserverteMelding())
                    }
                }
            }
            return mottatteMeldinger
        }

        private fun Application.sseTestApp(refreshKlienter: Channel<SseEvent>) {
            install(Locations)
            routing {
                get("/isready") {
                    call.respond(HttpStatusCode.OK)
                }
                Sse(sseChannel = sseChannel(refreshKlienter))
            }
        }

        private fun withNettyEngine(refreshKlienter: Channel<SseEvent>, block: suspend () -> Unit) {
            val server = embeddedServer(Netty, applicationEngineEnvironment {
                module { sseTestApp(refreshKlienter) }
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
}
