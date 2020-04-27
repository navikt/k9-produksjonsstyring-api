package no.nav.k9.aksjonspunktbehandling

import io.ktor.util.KtorExperimentalAPI
import no.nav.k9.Configuration
import no.nav.k9.domene.lager.oppgave.Oppgave
import no.nav.k9.domene.modell.FagsakYtelseType
import no.nav.k9.domene.modell.Modell
import no.nav.k9.domene.repository.BehandlingProsessEventRepository
import no.nav.k9.domene.repository.OppgaveRepository
import no.nav.k9.kafka.dto.BehandlingProsessEventDto
import no.nav.k9.sakogbehandling.SakOgBehadlingProducer
import no.nav.melding.virksomhet.behandlingsstatus.hendelsehandterer.v1.hendelseshandtererbehandlingsstatus.*
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.ZoneId
import java.util.*
import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.XMLGregorianCalendar


class K9sakEventHandler @KtorExperimentalAPI constructor(
    val oppgaveRepository: OppgaveRepository,
    val behandlingProsessEventRepository: BehandlingProsessEventRepository,
    val config: Configuration,
    val sakOgBehadlingProducer: SakOgBehadlingProducer
//    val gosysOppgaveGateway: GosysOppgaveGateway
) {
    private val log = LoggerFactory.getLogger(K9sakEventHandler::class.java)

    @KtorExperimentalAPI
    fun prosesser(
        event: BehandlingProsessEventDto
    ) {

        log.info(objectMapper().writeValueAsString(event))

        val modell = behandlingProsessEventRepository.lagre(event)


        val oppgave = modell.oppgave()
        // Sjekk om behandlingen starter eller avsluttes, skal da sende en melding til behandlesak for å fortelle modia.
        if (modell.starterSak()) {
            behandlingOpprettet(modell, sakOgBehadlingProducer)
        }

        if (!oppgave.aktiv) {
            behandlingAvsluttet(modell, sakOgBehadlingProducer)
        }

        oppgaveRepository.lagre(oppgave.eksternId) { forrigeOppgave: Oppgave? ->
            oppgave.reservasjon = forrigeOppgave?.reservasjon
            oppgave
        }

        log.info(objectMapper().writeValueAsString(modell))
        log.info(objectMapper().writeValueAsString(oppgave))

        // log.info(oppgave.datavarehusSak())
        // log.info(oppgave.datavarehusBehandling())

    }

    private fun behandlingOpprettet(
        modell: Modell,
        sakOgBehadlingProducer: SakOgBehadlingProducer
    ) {
        val applikasjoner =
            Applikasjoner()
        applikasjoner.value = "k9-sak"
        val primaerRelasjonstyper =
            PrimaerRelasjonstyper()
        primaerRelasjonstyper.value =
            "forrige" //Er fra kodeverk: http://nav.no/kodeverk/Kode/Prim_c3_a6rRelasjonstyper/forrige?v=1
        val aktoer = Aktoer()
        val sisteEvent = modell.sisteEvent()
        val fagsakYtelseType = FagsakYtelseType.fraKode(sisteEvent.ytelseTypeKode)
        aktoer.withAktoerId(sisteEvent.aktørId)
        val behandlingOpprettet = BehandlingOpprettet()
            .withBehandlingsID("k9-los-" + sisteEvent.behandlingId)
            .withBehandlingstema(
                Behandlingstemaer(
                    fagsakYtelseType.navn,
                    fagsakYtelseType.kode,
                    fagsakYtelseType.kodeverk
                )
            )
            .withHendelsesId(UUID.randomUUID().toString())
            .withHendelsesprodusentREF(applikasjoner)
            .withHendelsesTidspunkt(gregDate(sisteEvent.eventTid.toLocalDate()))
            .withBehandlingstype(
                Behandlingstyper(
                ).withKodeRef(sisteEvent.behandlingTypeKode)
            )
            .withAktoerREF(aktoer)
            .withSakstema(Sakstemaer().withKodeRef("k9 kode"))
            .withAnsvarligEnhetREF("NASJONAL")
        
      //  sakOgBehadlingProducer.opprettetBehandlng(no.nav.k9.kafka.Metadata(1, "", ""), objectMapper().writeValueAsString(behandlingOpprettet))
    }

    private fun behandlingAvsluttet(
        modell: Modell,
        sakOgBehadlingProducer: SakOgBehadlingProducer
    ) {
        val applikasjoner =
            Applikasjoner()
        applikasjoner.value = "k9-sak"
        val primaerRelasjonstyper =
            PrimaerRelasjonstyper()
        primaerRelasjonstyper.value =
            "forrige" //Er fra kodeverk: http://nav.no/kodeverk/Kode/Prim_c3_a6rRelasjonstyper/forrige?v=1
        val aktoer = Aktoer()
        aktoer.withAktoerId(modell.sisteEvent().aktørId)
        val behandlingAvsluttet = BehandlingAvsluttet()
            .withBehandlingsID("k9-los-" + modell.sisteEvent().behandlingId)
            .withBehandlingstema(Behandlingstemaer("", "", ""))
            .withHendelsesId(UUID.randomUUID().toString())
            .withHendelsesprodusentREF(applikasjoner)
            .withHendelsesTidspunkt(gregDate(modell.sisteEvent().eventTid.toLocalDate()))
            .withBehandlingstype(Behandlingstyper().withValue("aS"))
            .withAktoerREF(aktoer)
            .withSakstema(Sakstemaer())
            .withAnsvarligEnhetREF("NASJONAL")
      //  sakOgBehadlingProducer.avsluttetBehandling(no.nav.k9.kafka.Metadata(1, "", ""), objectMapper().writeValueAsString(behandlingAvsluttet))
       
    }

    private fun gregDate(localDate: LocalDate): XMLGregorianCalendar? {
        val gcal = GregorianCalendar.from(localDate.atStartOfDay(ZoneId.systemDefault()))
        return DatatypeFactory.newInstance().newXMLGregorianCalendar(gcal)
    }
}