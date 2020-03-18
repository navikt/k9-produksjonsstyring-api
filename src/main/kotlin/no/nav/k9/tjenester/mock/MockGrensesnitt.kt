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
                styleLink("/static/bootstrap.css")
                script(src = "/static/script.js") {}
            }
            body {
                div {
                    classes = setOf("container ")

                    h1 { +"Testside for k9-los" }
                    p {
                        +"Aksjonspunkter toggles av og på som hendelser. En behandling regnes som avsluttet dersom den er opprettet og ikke lengre har noen aksjonspunkter som operative"
                    }


                    div {
                        classes = setOf("input-group", "mb-3")
                        div {
                            classes = setOf("input-group-prepend")
                            span {
                                classes = setOf("input-group-text")
                                +"EksternId"
                            }
                        }
                        textInput {
                            classes = setOf("form-control")
                            id = "uuid"
                            value = UUID.randomUUID().toString()
                        }
                    }

                    div {
                        classes = setOf("input-group", "mb-3")
                        div {
                            classes = setOf("input-group-prepend")
                            span {
                                classes = setOf("input-group-text")
                                +"Aktørid"
                            }
                        }
                        textInput {
                            classes = setOf("form-control")
                            id = "aktørid"
                            value = "aktørid"
                        }
                    }

                    ul {
                        classes = setOf("list-group")
                        for (aksjonspunkt in Aksjonspunkter().aksjonspunkter()) {
                            li {
                                classes = setOf("list-group-item")
                                div { +"Kode: ${aksjonspunkt.kode}" }
                                div { +"Navn: ${aksjonspunkt.navn}" }
                                div { +"Behandlingsstegtype: ${aksjonspunkt.behandlingsstegtype}" }
                                div { +"Plassering: ${aksjonspunkt.plassering}" }
                                div { +"Totrinnsbehandling: ${aksjonspunkt.totrinn}" }
                                div {
                                    classes = setOf("form-check")
                                    checkBoxInput {
                                        classes = setOf("form-check-input")
                                        id = "Checkbox${aksjonspunkt.kode}"
                                        onClick = "toggle('${aksjonspunkt.kode}')"
                                    }
                                    label {
                                        classes = setOf("form-check-label")
                                        +"Toggle"
                                    }
                                }
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

        val modell = behandlingProsessEventRepository.hent(UUID.fromString(aksjonspunktToggle.eksternid))

        val event = if (modell.erTom()) {
            BehandlingProsessEventDto(
                UUID.fromString(aksjonspunktToggle.eksternid),
                Fagsystem.K9SAK,
                "Saksnummer",
                aksjonspunktToggle.aktørid,
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