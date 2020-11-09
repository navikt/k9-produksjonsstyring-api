package no.nav.k9.aksjonspunktbehandling

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.util.*
import no.nav.helse.dusseldorf.ktor.jackson.dusseldorfConfigured
import no.nav.k9.buildAndTestConfig
import no.nav.k9.domene.modell.Aksjonspunkter
import no.nav.k9.domene.repository.OppgaveRepository
import no.nav.k9.integrasjon.kafka.dto.BehandlingProsessEventDto
import no.nav.k9.integrasjon.kafka.dto.PunsjEventDto
import no.nav.k9.integrasjon.kafka.dto.PunsjId
import no.nav.k9.sak.typer.AktørId
import no.nav.k9.sak.typer.JournalpostId
import org.intellij.lang.annotations.Language
import org.junit.Rule
import org.junit.Test
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertFalse
import kotlin.test.assertTrue


class K9PunsjEventHandlerTest : KoinTest {

    private val log = LoggerFactory.getLogger(K9PunsjEventHandlerTest::class.java)

    @KtorExperimentalAPI
    @get:Rule
    val koinTestRule = KoinTestRule.create {
        modules(buildAndTestConfig())
    }

    @KtorExperimentalAPI
    @Test
    fun `Skal opprette en oppgave dersom en punsjoppgave har et aktivt aksjonspunkt`() {

        val k9PunsjEventHandler = get<K9punsjEventHandler>()
        val oppgaveRepository = get<OppgaveRepository>()

        @Language("JSON") val json =
            """{"eksternId":"871da8c5-a1a8-4353-a10b-10f6e4521490",
                "journalpostId":"585886",
                "eventTid":"2020-11-09T12:24:57.46806",
                "aktørId":"980458930",
                "aksjonspunkter":{"liste":{"OPPR":"Opprettet"}}}
            """.trimIndent()

        val punsjOppgave = PunsjEventDto(
            UUID.randomUUID(),
            JournalpostId("585886"),
            LocalDateTime.now(),
            AktørId("980458930"),
            Aksjonspunkter(mapOf("OPPR" to "Opprettet"))
        )
        val objectMapper = jacksonObjectMapper().dusseldorfConfigured()
        val event = objectMapper.readValue(json, PunsjEventDto::class.java)

        k9PunsjEventHandler.prosesser(event)
        val oppgaveModell = oppgaveRepository.hent(UUID.fromString(event.eksternId.toString()))
        val oppgave = oppgaveModell
        assertTrue { oppgave.aktiv }
    }
}
