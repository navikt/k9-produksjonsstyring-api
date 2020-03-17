package no.nav.k9.tjenester.mock

import io.ktor.application.call
import io.ktor.html.respondHtml
import io.ktor.http.HttpStatusCode
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.locations.post
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.util.KtorExperimentalAPI
import kotlinx.html.*
import no.nav.k9.aksjonspunktbehandling.K9sakEventHandler
import no.nav.k9.domene.repository.BehandlingProsessEventRepository
import no.nav.k9.kafka.dto.BehandlingProsessEventDto
import no.nav.k9.kafka.dto.EventHendelse
import no.nav.k9.kafka.dto.Fagsystem
import java.time.LocalDateTime
import java.util.*

@KtorExperimentalAPI
@KtorExperimentalLocationsAPI
fun Route.MockGrensesnitt(
    k9sakEventHandler: K9sakEventHandler,
    behandlingProsessEventRepository: BehandlingProsessEventRepository
) {
    @Location("/")
    class main

    get { _: main ->
        call.respondHtml {
            head {
                title { +"Test app for k9-los" }
                script(src = "/static/script.js") {}
            }
            body {
                p {
                    +"Aksjonspunkter toggles av og på som hendelser. En behandling regnes som "
                }

                ul {
                    for (aksjonspunkt in Aksjonspunkter().aksjonspunkter()) {
                        li {
                            div { +"Kode: ${aksjonspunkt.kode}" }
                            div { +"Navn: ${aksjonspunkt.navn}" }
                            div { +"Behandlingsstegtype: ${aksjonspunkt.behandlingsstegtype}" }
                            div { +"Plassering: ${aksjonspunkt.plassering}" }
                            div { +"Totrinnsbehandling: ${aksjonspunkt.totrinn.toString()}" }
                            checkBoxInput {
                                id = "Checkbox${aksjonspunkt.kode}"
                                onClick = "toggle('${aksjonspunkt.kode}')"
                            }
                        }
                    }
                }
            }
        }
    }

    @Location("/toggleaksjonspunkt")
    class aksjonspunkt

    post { _: aksjonspunkt ->
        val aksjonspunktToggle = call.receive<AksjonspunktToggle>()
        println(aksjonspunktToggle)

        val aksjonspunkt =
            Aksjonspunkter().aksjonspunkter().find { aksjonspunkt -> aksjonspunkt.kode == aksjonspunktToggle.kode }!!


        val uuid = UUID.fromString("d86ffe71-0137-4fe6-97bc-237f504935ab")
        val modell = behandlingProsessEventRepository.hent(uuid)

        val event = if (modell.erTom()) {
            BehandlingProsessEventDto(
                uuid,
                Fagsystem.K9SAK,
                "Saksnummer",
                "",
                1234L,
                LocalDateTime.now(),
                EventHendelse.AKSJONSPUNKT_OPPRETTET,
                behandlingStatus = "UTRED",
                aksjonspunktKoderMedStatusListe = mutableMapOf(aksjonspunktToggle.kode to "OPPR"),
                behandlendeEnhet = "2020",
                behandlingSteg = "",
                opprettetBehandling = LocalDateTime.now(),
                behandlingTypeKode = "BT-005",
                ytelseTypeKode = "PSB"
            )
        } else {
            val sisteEvent = modell.sisteEvent()
            sisteEvent.aksjonspunktKoderMedStatusListe[aksjonspunktToggle.kode] =
                if (aksjonspunktToggle.toggle) "OPPR" else "AVSL"
            BehandlingProsessEventDto(
                sisteEvent.eksternId,
                sisteEvent.fagsystem,
                sisteEvent.saksnummer,
                sisteEvent.aktørId,
                sisteEvent.behandlingId,
                LocalDateTime.now(),
                EventHendelse.AKSJONSPUNKT_OPPRETTET,
                behandlingStatus = sisteEvent.behandlingStatus,
                aksjonspunktKoderMedStatusListe = sisteEvent.aksjonspunktKoderMedStatusListe,
                behandlendeEnhet = sisteEvent.behandlendeEnhet,
                behandlingSteg = "",
                opprettetBehandling = LocalDateTime.now(),
                behandlingTypeKode = "BT-005",
                ytelseTypeKode = sisteEvent.ytelseTypeKode
            )
        }
        k9sakEventHandler.prosesser(event)

        call.respond(HttpStatusCode.Accepted)
    }
}