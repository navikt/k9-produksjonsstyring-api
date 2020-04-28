package no.nav.k9.aksjonspunktbehandling

import io.ktor.util.KtorExperimentalAPI
import no.nav.k9.Configuration
import no.nav.k9.domene.lager.oppgave.Oppgave
import no.nav.k9.domene.modell.Modell
import no.nav.k9.domene.repository.BehandlingProsessEventRepository
import no.nav.k9.domene.repository.OppgaveRepository
import no.nav.k9.kafka.dto.BehandlingProsessEventDto
import no.nav.k9.sakogbehandling.SakOgBehadlingProducer
import no.nav.k9.sakogbehandling.kontrakt.BehandlingAvsluttet
import no.nav.k9.sakogbehandling.kontrakt.BehandlingOpprettet
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
    private val `Omsorgspenger, Pleiepenger og opplæringspenger` = "ab0271"
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
            behandlingAvsluttet(modell)
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
        val sisteEvent = modell.sisteEvent()
        val behandlingOpprettet = BehandlingOpprettet(
            hendelseType = "behandlingOpprettet",
            hendelsesId = UUID.randomUUID().toString(),
            hendelsesprodusentREF = BehandlingOpprettet.HendelsesprodusentREF("", "", ""),
            hendelsesTidspunkt = sisteEvent.eventTid,
            behandlingsID = ("k9-los-" + sisteEvent.behandlingId),
            behandlingstype = BehandlingOpprettet.Behandlingstype("", "", sisteEvent.behandlingTypeKode),
            sakstema = BehandlingOpprettet.Sakstema("", "", "OMS"),
            behandlingstema = BehandlingOpprettet.Behandlingstema("", "", `Omsorgspenger, Pleiepenger og opplæringspenger`),
            aktoerREF = listOf(BehandlingOpprettet.AktoerREF(sisteEvent.aktørId)),
            ansvarligEnhetREF = "NASJONAL",
            primaerBehandlingREF = BehandlingOpprettet.PrimaerBehandlingREF(
                "",
                BehandlingOpprettet.PrimaerBehandlingREF.Type("", "", "")
            ),
            sekundaerBehandlingREF = listOf(),
            applikasjonSakREF = "",
            applikasjonBehandlingREF = "",
            styringsinformasjonListe = listOf()
        )

        sakOgBehadlingProducer.opprettetBehandlng(no.nav.k9.kafka.Metadata(1, "", ""),behandlingOpprettet)
    }

    private fun behandlingAvsluttet(
        modell: Modell
//        sakOgBehadlingProducer: SakOgBehadlingProducer
    ) {
        val sisteEvent = modell.sisteEvent()
        val behandlingAvsluttet = BehandlingAvsluttet(
            hendelseType = "behandlingAvsluttet",
            hendelsesId = UUID.randomUUID().toString(),
            hendelsesprodusentREF = BehandlingAvsluttet.HendelsesprodusentREF("", "", ""),
            hendelsesTidspunkt = sisteEvent.eventTid,
            behandlingsID = ("k9-los-" + sisteEvent.behandlingId),
            behandlingstype = BehandlingAvsluttet.Behandlingstype("", "", sisteEvent.behandlingTypeKode),
            sakstema = BehandlingAvsluttet.Sakstema("", "", "OMS"),
            behandlingstema = BehandlingAvsluttet.Behandlingstema("", "", `Omsorgspenger, Pleiepenger og opplæringspenger`),
            aktoerREF = listOf(BehandlingAvsluttet.AktoerREF(sisteEvent.aktørId)),
            ansvarligEnhetREF = "NASJONAL",
            primaerBehandlingREF = BehandlingAvsluttet.PrimaerBehandlingREF(
                "",
                BehandlingAvsluttet.PrimaerBehandlingREF.Type("", "", "")
            ),
            sekundaerBehandlingREF = listOf(),
            applikasjonSakREF = "",
            applikasjonBehandlingREF = "",
            styringsinformasjonListe = listOf(),
            avslutningsstatus = BehandlingAvsluttet.Avslutningsstatus("", "", "")
        )

        //  sakOgBehadlingProducer.avsluttetBehandling(no.nav.k9.kafka.Metadata(1, "", ""), objectMapper().writeValueAsString(behandlingAvsluttet))

    }

    private fun gregDate(localDate: LocalDate): XMLGregorianCalendar? {
        val gcal = GregorianCalendar.from(localDate.atStartOfDay(ZoneId.systemDefault()))
        return DatatypeFactory.newInstance().newXMLGregorianCalendar(gcal)
    }
}