package no.nav.k9.tjenester.innsikt

import io.ktor.application.*
import io.ktor.html.*
import io.ktor.locations.*
import io.ktor.routing.*
import io.ktor.util.*
import kotlinx.html.*
import no.nav.k9.domene.modell.BehandlingStatus
import no.nav.k9.domene.modell.BehandlingType
import no.nav.k9.domene.modell.FagsakYtelseType
import no.nav.k9.domene.modell.OppgaveKø
import no.nav.k9.domene.repository.*
import no.nav.k9.tjenester.avdelingsleder.nokkeltall.NokkeltallTjeneste
import no.nav.k9.tjenester.saksbehandler.oppgave.OppgaveTjeneste
import org.koin.ktor.ext.inject
import kotlin.streams.toList

@KtorExperimentalAPI
@KtorExperimentalLocationsAPI
fun Route.innsiktGrensesnitt() {
    val oppgaveRepository by inject<OppgaveRepository>()
    val oppgaveKøRepository by inject<OppgaveKøRepository>()
    val saksbehandlerRepository by inject<SaksbehandlerRepository>()
    val behandlingProsessEventK9Repository by inject<BehandlingProsessEventK9Repository>()
    val nøkkeltjeneste by inject<NokkeltallTjeneste>()
    val oppgaveTjeneste by inject<OppgaveTjeneste>()
    val statistikkRepository by inject<StatistikkRepository>()

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

                    val avsluttede = oppgaveRepository.hentMedBehandlingsstatus(BehandlingStatus.AVSLUTTET)
                    val inaktiveIkkeAvsluttedeOppgaver = oppgaveRepository.hentInaktiveIkkeAvluttet()

                    /**
                     *  AVSLUTTET("AVSLU", "Avsluttet"),
                    FATTER_VEDTAK("FVED", "Fatter vedtak"),
                    IVERKSETTER_VEDTAK("IVED", "Iverksetter vedtak"),
                    OPPRETTET("OPPRE", "Opprettet"),
                    UTREDES("UTRED", "Utredes");
                     */
                    val fatterVedtakAvsluttet =
                        oppgaveRepository.hentInaktiveIkkeAvluttetMedBehandlingStatus(BehandlingStatus.FATTER_VEDTAK)
                    val iverksetterVedtakAvsluttet =
                        oppgaveRepository.hentInaktiveIkkeAvluttetMedBehandlingStatus(BehandlingStatus.IVERKSETTER_VEDTAK)
                    val opprettetAvsluttet =
                        oppgaveRepository.hentInaktiveIkkeAvluttetMedBehandlingStatus(BehandlingStatus.OPPRETTET)
                    val utredesAvsluttet =
                        oppgaveRepository.hentInaktiveIkkeAvluttetMedBehandlingStatus(BehandlingStatus.UTREDES)
                    val automatiskProsesserteTotalt = oppgaveRepository.hentAutomatiskProsesserteTotalt()
                    val aksjonspunkter = oppgaveRepository.hentAktiveOppgaversAksjonspunktliste()
                    val oppgaverTotaltAktive = oppgaveRepository.hentAktiveOppgaverTotaltIkkeSkjermede()
                    p {
                        +"Det er nå ${aksjonspunkter.sumBy { it.antall }} åpne aksjonspunkter fordelt på $oppgaverTotaltAktive oppgaver, $inaktiveIkkeAvsluttedeOppgaver inaktive med annen status enn avsluttet (fatter vedtak ${fatterVedtakAvsluttet}, iverksetter vedtak ${iverksetterVedtakAvsluttet}, opprettet ${opprettetAvsluttet}, utredes ${utredesAvsluttet}) og $avsluttede med status avsluttet, $automatiskProsesserteTotalt er prosessert automatisk"
                    }
                    p {
                        +"Totalt ${oppgaverTotaltAktive + inaktiveIkkeAvsluttedeOppgaver + avsluttede}"
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
                        +"Statistikk kommer under her:"
                    }

                    val hentBeholdningAvOppgaverPerAntallDager =
                        oppgaveTjeneste.hentBeholdningAvOppgaverPerAntallDager()

                    p {
                        +"Beholdning Av Oppgaver Per Antall Dager:"
                    }

                    for (data in hentBeholdningAvOppgaverPerAntallDager) {
                        if (data.fagsakYtelseType == FagsakYtelseType.OMSORGSPENGER) {
                            div {
                                classes = setOf("input-group-text display-4")
                                +"dato: ${data.dato} antall: ${data.antall} fagsakType: ${data.fagsakYtelseType} BehandlingsType: ${data.behandlingType}"
                            }
                        }
                    }

                    val hentFerdigstilteOgNyeHistorikkMedYtelsetypeSiste8Uker =
                        statistikkRepository.hentFerdigstilteOgNyeHistorikkMedYtelsetypeSiste8Uker(true)


                    val antallNyeFørstegangs = hentFerdigstilteOgNyeHistorikkMedYtelsetypeSiste8Uker
                        .stream()
                        .filter { entry -> entry.fagsakYtelseType == FagsakYtelseType.OMSORGSPENGER && entry.behandlingType == BehandlingType.FORSTEGANGSSOKNAD }
                        .map { data -> data.nye.size }
                        .toList()
                        .reduce { acc: Int, i: Int -> acc + i }

                    val antallNyeRev = hentFerdigstilteOgNyeHistorikkMedYtelsetypeSiste8Uker
                        .stream()
                        .filter { entry -> entry.fagsakYtelseType == FagsakYtelseType.OMSORGSPENGER && entry.behandlingType == BehandlingType.REVURDERING }
                        .map { data -> data.nye.size }
                        .toList()
                        .reduce { acc: Int, i: Int -> acc + i }

                    val antallFerSok = hentFerdigstilteOgNyeHistorikkMedYtelsetypeSiste8Uker
                        .stream()
                        .filter { entry -> entry.fagsakYtelseType == FagsakYtelseType.OMSORGSPENGER && entry.behandlingType == BehandlingType.FORSTEGANGSSOKNAD }
                        .map { data -> data.ferdigstilte.size }
                        .toList()
                        .reduce { acc: Int, i: Int -> acc + i }

                    val antallFerRev = hentFerdigstilteOgNyeHistorikkMedYtelsetypeSiste8Uker
                        .stream()
                        .filter { entry -> entry.fagsakYtelseType == FagsakYtelseType.OMSORGSPENGER && entry.behandlingType == BehandlingType.REVURDERING }
                        .map { data -> data.ferdigstilte.size }
                        .toList()
                        .reduce { acc: Int, i: Int -> acc + i }

                   div {
                        classes = setOf("input-group-text display-4")
                        +"antallNyeFørstegangs: ${antallNyeFørstegangs} antallNyeRev: ${antallNyeRev} antallFerSok: ${antallFerSok} antallFerRev: ${antallFerRev}"
                    }
                }
            }
        }
    }

    var køer = listOf<OppgaveKø>()

    @Location("/db")
    class db
    get { _: db ->
        if (køer.isEmpty()) {
            val alleReservasjoner =
                saksbehandlerRepository.hentAlleSaksbehandlereIkkeTaHensyn().flatMap { it.reservasjoner }
            val hentAktiveOppgaver =
                oppgaveRepository.hentAktiveOppgaver().filterNot { alleReservasjoner.contains(it.eksternId) }

            val k = oppgaveKøRepository.hentIkkeTaHensyn()
            for (b in k.filter { !it.kode6 }) {
                b.oppgaverOgDatoer.clear()
                for (oppgave in hentAktiveOppgaver) {
                    b.leggOppgaveTilEllerFjernFraKø(oppgave)
                }
            }
            køer = k
            call.respondHtml { }
        } else {
            call.respondHtml {
                head {
                    title { +"Innsikt i k9-los" }
                    styleLink("/static/bootstrap.css")
                    script(src = "/static/script.js") {}
                }
                body {
                    val list =
                        oppgaveKøRepository.hentIkkeTaHensyn().filter { !it.kode6 }
                    ul {
                        for (l in list) {
                            val oppgaverOgDatoer = køer.first { it.navn == l.navn }.oppgaverOgDatoer
                            val size = oppgaverOgDatoer.size
                            oppgaverOgDatoer.removeAll(l.oppgaverOgDatoer)

                            li {
                                +"${l.navn}: ${l.oppgaverOgDatoer.size} vs $size"
                            }
                        }
                    }

                    ul {
                        for (mutableEntry in Databasekall.map.entries.toList()
                            .sortedByDescending { mutableEntry -> mutableEntry.value.sum() }) {
                            li {
                                +"${mutableEntry.key}: ${mutableEntry.value} "
                            }
                        }
                    }
                    køer = emptyList()
                }
            }

        }
    }
}
