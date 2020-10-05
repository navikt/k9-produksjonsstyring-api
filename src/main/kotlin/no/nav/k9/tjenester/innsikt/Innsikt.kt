package no.nav.k9.tjenester.innsikt

import io.ktor.application.*
import io.ktor.html.*
import io.ktor.http.*
import io.ktor.locations.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.html.*
import no.nav.k9.domene.repository.*
import no.nav.k9.utils.Cache
import no.nav.k9.utils.CacheObject
import org.koin.ktor.ext.inject
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore

@KtorExperimentalAPI
@KtorExperimentalLocationsAPI
fun Route.innsiktGrensesnitt() {
    val oppgaveRepository by inject<OppgaveRepository>()
    val oppgaveKøRepository by inject<OppgaveKøRepository>()
    val saksbehandlerRepository by inject<SaksbehandlerRepository>()
    val behandlingProsessEventRepository by inject<BehandlingProsessEventK9Repository>()
    val reservasjonRepository by inject<ReservasjonRepository>()

    @Location("/")
    class main

    get { _: main ->
        call.respondHtml {

            head {
                title { +"Innsikt i k9-los" }
                styleLink("/static/bootstrap.css")
                script(src = "/static/script.js") {}
            }
            body {
                div {
                    classes = setOf("container ")

                    h1 { +"Innsikt i k9-los" }
                    p {
                        +"Antall åpne oppgaver fordelt på aksjonspunkt."
                    }

                    val avsluttede = oppgaveRepository.hentAvsluttede()
                    val inaktiveIkkeAvsluttedeOppgaver = oppgaveRepository.hentInaktiveIkkeAvluttedeAvsluttede()
                    val automatiskProsesserteTotalt = oppgaveRepository.hentAutomatiskProsesserteTotalt()
                    val aksjonspunkter = oppgaveRepository.hentAktiveOppgaversAksjonspunktliste()
                    val oppgaverTotaltAktive = oppgaveRepository.hentAktiveOppgaverTotaltIkkeSkjermede()
                    val eldsteEventTidspunkt = behandlingProsessEventRepository.eldsteEventTid()
                    p {
                        +"Det er nå ${aksjonspunkter.sumBy { it.antall }} åpne aksjonspunkter fordelt på $oppgaverTotaltAktive oppgaver, $inaktiveIkkeAvsluttedeOppgaver inaktive med annen status enn avsluttet og $avsluttede med status avsluttet, $automatiskProsesserteTotalt er prosessert automatisk"
                    }
                    p {
                        +"Totalt ${oppgaverTotaltAktive + inaktiveIkkeAvsluttedeOppgaver + avsluttede}"
                    }
                    p {
                        +"Eldste eventTid kom $eldsteEventTidspunkt"
                    }
                    val list =
                        oppgaveKøRepository.hentIkkeTaHensyn().filter { !it.kode6 }.map { it.oppgaverOgDatoer.size }
                    p {
                        +"Kølengder ${list.joinToString()}"
                    }
                    for (aksjonspunkt in aksjonspunkter.stream().sorted { o1, o2 -> o2.antall.compareTo(o1.antall) }) {
                        if (aksjonspunkt.antall == 0) {
                            continue
                        }
                        div {
                            classes = setOf("input-group-text display-4")
                            +"${aksjonspunkt.antall} kode: ${aksjonspunkt.kode} ${aksjonspunkt.navn} Totrinn: ${aksjonspunkt.totrinn}"
                        }
                    }
                    
                    p {
                        + "${Databasekall.map}"
                    }
                    
                }
            }
        }
    }
    @Location("/mapping")
    class mapping

    get { _: mapping ->
        call.response.header(
            HttpHeaders.ContentDisposition,
            "attachment; filename=\"mapping_behandlingsid_externid.json\""
        )
        val mapMellomeksternIdOgBehandlingsid =
            behandlingProsessEventRepository.mapMellomeksternIdOgBehandlingsid()
        call.respond(mapMellomeksternIdOgBehandlingsid)
    }
    val cache = Cache<List<MappingEnhet>>(1)
    val semaphore = Semaphore(1)

    @Location("/mappingEnhet")
    class mappingEnhet
    get { _: mappingEnhet ->
        call.response.header(
            HttpHeaders.ContentDisposition,
            "attachment; filename=\"mapping_enhet_externid.json\""
        )
        if (cache.get("default") != null) {
            call.respond(cache.get("default")!!.value)
        } else {
            if (semaphore.tryAcquire()) {
                launch(Executors.newSingleThreadExecutor().asCoroutineDispatcher()) {
                    val list = mutableListOf<MappingEnhet>()
                    for (s in behandlingProsessEventRepository.hentAlleEventerIder()) {
                        val enhet = if (reservasjonRepository.finnes(UUID.fromString(s))) {
                            val hentMedHistorikk = reservasjonRepository.hentMedHistorikk(UUID.fromString(s))
                            val reservertav = hentMedHistorikk
                                .map { reservasjon -> reservasjon.reservertAv }.first()
                            saksbehandlerRepository.finnSaksbehandlerMedIdentIkkeTaHensyn(reservertav)?.enhet?.substringBefore(
                                " "
                            )
                        } else {
                            "SRV"
                        }
                        list.add(MappingEnhet(s, enhet))
                    }
                    cache.set("default", CacheObject(list, LocalDateTime.now().plusDays(2)))
                    semaphore.release()
                }
            }
            call.respond(HttpStatusCode.ServiceUnavailable)
        }
    }

    @Location("/overflow")
    class overflow

    get { _: overflow ->
        val oppgaveIder = oppgaveRepository.hentAktiveOppgaver().map { it.eksternId }.toSet()

        val mutableSet = oppgaveIder.toMutableSet()


        mutableSet
            .removeAll(oppgaveKøRepository.hentIkkeTaHensyn().flatMap { it.oppgaverOgDatoer }.map { it.id }.toSet())
        val reservasjoner = saksbehandlerRepository.hentAlleSaksbehandlereIkkeTaHensyn().flatMap { it.reservasjoner }
        mutableSet.removeAll(
            reservasjonRepository.hentSelvOmDeIkkeErAktive(reservasjoner.toSet()).filter { it.erAktiv() }
                .map { it.oppgave })

        val oppgaver = oppgaveRepository.hentOppgaver(mutableSet)
        if (oppgaver.isEmpty()) {
            call.respond("Ingen overflødige")
        } else {
            call.respond(oppgaver.size)
        }
    }
}