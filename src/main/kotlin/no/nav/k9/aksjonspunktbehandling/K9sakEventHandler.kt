package no.nav.k9.aksjonspunktbehandling

import io.ktor.util.KtorExperimentalAPI
import no.nav.k9.domene.modell.Modell
import no.nav.k9.domene.repository.BehandlingProsessEventRepository
import no.nav.k9.domene.repository.OppgaveRepository
import no.nav.k9.integrasjon.sakogbehandling.sendBehandlingAvsluttet
import no.nav.k9.integrasjon.sakogbehandling.sendBehandlingOpprettet
import no.nav.melding.virksomhet.behandlingsstatus.hendelsehandterer.v1.hendelseshandtererbehandlingsstatus.*
import no.nav.vedtak.felles.integrasjon.kafka.BehandlingProsessEventDto

import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.ZoneId
import java.util.*
import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.XMLGregorianCalendar


class K9sakEventHandler @KtorExperimentalAPI constructor(
    val oppgaveRepository: OppgaveRepository,
    val behandlingProsessEventRepository: BehandlingProsessEventRepository
//    val gosysOppgaveGateway: GosysOppgaveGateway
) {
    private val log = LoggerFactory.getLogger(K9sakEventHandler::class.java)

    @KtorExperimentalAPI
    fun prosesser(event: BehandlingProsessEventDto) {
        val modell = behandlingProsessEventRepository.lagre(event)


        // Sjekk om behandlingen starter eller avsluttes, skal da sende en melding til behandlesak for å fortelle modia.
        if (false) {
            behandlingOpprettet(modell)
            behandlingAvsluttet(modell)
        }


        val oppgave = modell.oppgave()
        oppgaveRepository.lagre(oppgave)
    }

    private fun behandlingOpprettet(modell: Modell) {
        val applikasjoner =
            Applikasjoner()
        applikasjoner.value = "k9-sak"
        val primaerRelasjonstyper =
            PrimaerRelasjonstyper()
        primaerRelasjonstyper.value =
            "forrige" //Er fra kodeverk: http://nav.no/kodeverk/Kode/Prim_c3_a6rRelasjonstyper/forrige?v=1
        val aktoer = Aktoer()
        aktoer.withAktoerId(modell.sisteEvent().aktørId)
        sendBehandlingOpprettet(
            BehandlingOpprettet()
                .withBehandlingsID("k9-los-" + modell.sisteEvent().behandlingId)
                .withBehandlingstema(Behandlingstemaer("", "", ""))
                .withHendelsesId(UUID.randomUUID().toString())
                .withHendelsesprodusentREF(applikasjoner)
                .withHendelsesTidspunkt(gregDate(modell.sisteEvent().eventTid!!.toLocalDate()))
                .withBehandlingstype(Behandlingstyper().withValue("aS"))
                .withAktoerREF(aktoer)
                .withSakstema(Sakstemaer())
                .withAnsvarligEnhetREF("")
        )
    }

    private fun behandlingAvsluttet(modell: Modell) {
        val applikasjoner =
            Applikasjoner()
        applikasjoner.value = "k9-sak"
        val primaerRelasjonstyper =
            PrimaerRelasjonstyper()
        primaerRelasjonstyper.value =
            "forrige" //Er fra kodeverk: http://nav.no/kodeverk/Kode/Prim_c3_a6rRelasjonstyper/forrige?v=1
        val aktoer = Aktoer()
        aktoer.withAktoerId(modell.sisteEvent().aktørId)
        sendBehandlingAvsluttet(
            BehandlingAvsluttet()
                .withBehandlingsID("k9-los-" + modell.sisteEvent().behandlingId)
                .withBehandlingstema(Behandlingstemaer("", "", ""))
                .withHendelsesId(UUID.randomUUID().toString())
                .withHendelsesprodusentREF(applikasjoner)
                .withHendelsesTidspunkt(gregDate(modell.sisteEvent().eventTid!!.toLocalDate()))
                .withBehandlingstype(Behandlingstyper().withValue("aS"))
                .withAktoerREF(aktoer)
                .withSakstema(Sakstemaer())
                .withAnsvarligEnhetREF("")
        )
    }

    private fun gregDate(localDate: LocalDate): XMLGregorianCalendar? {
        val gcal = GregorianCalendar.from(localDate.atStartOfDay(ZoneId.systemDefault()))
        return DatatypeFactory.newInstance().newXMLGregorianCalendar(gcal)
    }
}