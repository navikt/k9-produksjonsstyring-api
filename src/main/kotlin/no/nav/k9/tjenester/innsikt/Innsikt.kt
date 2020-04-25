package no.nav.k9.tjenester.innsikt

import io.ktor.application.call
import io.ktor.html.respondHtml
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.routing.Route
import io.ktor.util.KtorExperimentalAPI
import kotlinx.html.*
import no.nav.k9.domene.repository.OppgaveRepository
import no.nav.k9.tjenester.mock.Aksjonspunkter

@KtorExperimentalAPI
@KtorExperimentalLocationsAPI
fun Route.InnsiktGrensesnitt(
    oppgaveRepository: OppgaveRepository
) {
    @Location("/")
    class main

    get { _: main ->
        call.respondHtml {

            head {
                title { +"Innsikt i  k9-los" }
                styleLink("/static/bootstrap.css")
                script(src = "/static/script.js") {}
            }
            body {
                div {
                    classes = setOf("container ")

                    h1 { +"Innsikt i k9-los" }
                    p {
                        +"Antall åpne oppgaver fordelt på aksjonspunkt"
                    }

                    val aktiveOppgaver = oppgaveRepository.hentAktiveOppgaver()


                    val aksjonspunkter = Aksjonspunkter().aksjonspunkter()
                    for (aksjonspunkt in aksjonspunkter) {
                        aksjonspunkt.antall = aktiveOppgaver.filter { oppgaveModell ->
                            oppgaveModell.sisteOppgave().aksjonspunkter.liste.containsKey(
                                aksjonspunkt.kode
                            )
                        }.size
                    }
                    
                    for (aksjonspunkt in aksjonspunkter.stream().sorted { o1, o2 -> o2.antall.compareTo(o1.antall) }) {
                        if (aksjonspunkt.antall == 0){
                            continue
                        }
                        div {
                            div {
                                classes = setOf("input-group-text display-4")
                                +"${aksjonspunkt.antall} kode: ${aksjonspunkt.kode} ${aksjonspunkt.navn} Totrinn: ${aksjonspunkt.totrinn}"
                            }
                        }
                    }
                }
            }
        }
    }
}