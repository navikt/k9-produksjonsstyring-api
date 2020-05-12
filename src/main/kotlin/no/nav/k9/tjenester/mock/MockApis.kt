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
import java.time.LocalDate
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
                            value = "26104500284"
                        }
                    }

                    for (aksjonspunkt in Aksjonspunkter().aksjonspunkter()) {
                        div {
                            classes = setOf("input-group")
                            div {
                                classes = setOf("input-group-prepend")
                                div {
                                    classes = setOf("input-group-text")
                                    input(InputType.checkBox) {
                                        id = "Checkbox${aksjonspunkt.kode}"
                                        onClick = "toggle('${aksjonspunkt.kode}')"
                                    }
                                }
                            }
                            div {
                                classes = setOf("input-group-text display-4")
                                +"${aksjonspunkt.kode} ${aksjonspunkt.navn} Type: ${aksjonspunkt.behandlingsstegtype} Plassering: ${aksjonspunkt.plassering} Totrinn: ${aksjonspunkt.totrinn}"
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

        val modell = behandlingProsessEventRepository.hent(UUID.fromString(aksjonspunktToggle.eksternid))

        val event = if (modell.erTom()) {
            BehandlingProsessEventDto(
                UUID.fromString(aksjonspunktToggle.eksternid),
                Fagsystem.K9SAK,
                "Saksnummer",
                aksjonspunktToggle.aktørid,
                1234L,
                LocalDate.now(),
                LocalDateTime.now(),
                EventHendelse.AKSJONSPUNKT_OPPRETTET,
                behandlingStatus = "UTRED",
                aksjonspunktKoderMedStatusListe = mutableMapOf(aksjonspunktToggle.kode to "OPPR"),
                behandlingSteg = "",
                opprettetBehandling = LocalDateTime.now(),
                behandlingTypeKode = "BT-002",
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
                LocalDate.now(),
                LocalDateTime.now(),
                EventHendelse.AKSJONSPUNKT_OPPRETTET,
                behandlingStatus = sisteEvent.behandlingStatus,
                aksjonspunktKoderMedStatusListe = sisteEvent.aksjonspunktKoderMedStatusListe,
                behandlingSteg = "",
                opprettetBehandling = LocalDateTime.now(),
                behandlingTypeKode = "BT-002",
                ytelseTypeKode = sisteEvent.ytelseTypeKode
            )
        }
        k9sakEventHandler.prosesser(event)

        call.respond(HttpStatusCode.Accepted)
    }

    @Location("/10000AktiveEventer")
    class aksjonspunkt2

    get { _: aksjonspunkt2 ->
        for (i in 0..10000 step 1) {

            val event =
                BehandlingProsessEventDto(
                    UUID.randomUUID(),
                    Fagsystem.K9SAK,
                    "Saksnummer",
                    UUID.randomUUID().toString(),
                    1234L,
                    LocalDate.now(),
                    LocalDateTime.now(),
                    EventHendelse.AKSJONSPUNKT_OPPRETTET,
                    behandlingStatus = "UTRED",
                    aksjonspunktKoderMedStatusListe = mutableMapOf("5003" to "OPPR"),
                    behandlingSteg = "",
                    opprettetBehandling = LocalDateTime.now(),
                    behandlingTypeKode = "BT-002",
                    ytelseTypeKode = "PSB"
                )

            k9sakEventHandler.prosesser(event)
        }
        call.respond(HttpStatusCode.Accepted)
    }
}