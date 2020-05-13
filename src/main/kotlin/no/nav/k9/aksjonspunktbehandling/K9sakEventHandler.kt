package no.nav.k9.aksjonspunktbehandling

import io.ktor.util.KtorExperimentalAPI
import no.nav.k9.Configuration
import no.nav.k9.domene.modell.BehandlingType
import no.nav.k9.domene.modell.Modell
import no.nav.k9.domene.repository.BehandlingProsessEventRepository
import no.nav.k9.domene.repository.OppgaveKøRepository
import no.nav.k9.domene.repository.OppgaveRepository
import no.nav.k9.domene.repository.ReservasjonRepository
import no.nav.k9.integrasjon.sakogbehandling.SakOgBehadlingProducer
import no.nav.k9.integrasjon.sakogbehandling.kontrakt.BehandlingAvsluttet
import no.nav.k9.integrasjon.sakogbehandling.kontrakt.BehandlingOpprettet
import no.nav.k9.kafka.dto.BehandlingProsessEventDto
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
    val sakOgBehadlingProducer: SakOgBehadlingProducer,
    val oppgaveKøRepository: OppgaveKøRepository,
    val reservasjonRepository: ReservasjonRepository
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

        if (!config.erLokalt()) {
            if (modell.starterSak()) {
                behandlingOpprettet(modell, sakOgBehadlingProducer)
            }

            if (oppgave.behandlingStatus.navn == "AVSLUTTET" ) {
                behandlingAvsluttet(modell, sakOgBehadlingProducer)
            }
        }

        oppgaveRepository.lagre(oppgave.eksternId) {
            oppgave
        }

        for (oppgavekø in oppgaveKøRepository.hent()) {
            oppgaveKøRepository.lagre(oppgavekø.id) { forrige ->
                forrige?.leggOppgaveTilEllerFjernFraKø(oppgave, reservasjonRepository)
                forrige!!
            }
        }
    }

    @KtorExperimentalAPI
    private fun behandlingOpprettet(
        modell: Modell,
        sakOgBehadlingProducer: SakOgBehadlingProducer
    ) {
        val sisteEvent = modell.sisteEvent()
        val behandlingOpprettet = BehandlingOpprettet(
            hendelseType = "behandlingOpprettet",
            hendelsesId = sisteEvent.eksternId.toString() +"_"+ modell.eventer.size,
            hendelsesprodusentREF = BehandlingOpprettet.HendelsesprodusentREF("", "", "FS39"),
            hendelsesTidspunkt = sisteEvent.eventTid,
            behandlingsID = ("k9-los-" + sisteEvent.behandlingId),
            behandlingstype = BehandlingOpprettet.Behandlingstype("", "", BehandlingType.fraKode(sisteEvent.behandlingTypeKode).kodeverk),
            sakstema = BehandlingOpprettet.Sakstema("", "", "OMS"),
            behandlingstema = BehandlingOpprettet.Behandlingstema(
                "ab0149",
                "ab0149",
                `Omsorgspenger, Pleiepenger og opplæringspenger`
            ),
            aktoerREF = listOf(BehandlingOpprettet.AktoerREF(sisteEvent.aktørId)),
            ansvarligEnhetREF = "NASJONAL",
            primaerBehandlingREF = null,
            sekundaerBehandlingREF = listOf(),
            applikasjonSakREF = modell.sisteEvent().saksnummer,
            applikasjonBehandlingREF = modell.sisteEvent().eksternId.toString(),
            styringsinformasjonListe = listOf()
        )

        sakOgBehadlingProducer.opprettetBehandlng(behandlingOpprettet)
    }

    @KtorExperimentalAPI
    private fun behandlingAvsluttet(
        modell: Modell,
        sakOgBehadlingProducer: SakOgBehadlingProducer
    ) {
        val sisteEvent = modell.sisteEvent()
        val behandlingAvsluttet = BehandlingAvsluttet(
            hendelseType = "behandlingAvsluttet",
            hendelsesId = """${sisteEvent.eksternId.toString()}_${modell.eventer.size}""",
            hendelsesprodusentREF = BehandlingAvsluttet.HendelsesprodusentREF("", "", "FS39"),
            hendelsesTidspunkt = sisteEvent.eventTid,
            behandlingsID = ("k9-los-" + sisteEvent.behandlingId),
            behandlingstype = BehandlingAvsluttet.Behandlingstype("", "", BehandlingType.fraKode(sisteEvent.behandlingTypeKode).kodeverk),
            sakstema = BehandlingAvsluttet.Sakstema("", "", "OMS"),
            behandlingstema = BehandlingAvsluttet.Behandlingstema(
                "ab0149",
                "ab0149",
                `Omsorgspenger, Pleiepenger og opplæringspenger`
            ),
            aktoerREF = listOf(BehandlingAvsluttet.AktoerREF(sisteEvent.aktørId)),
            ansvarligEnhetREF = "NASJONAL",
            primaerBehandlingREF =  null,
            sekundaerBehandlingREF = listOf(),
            applikasjonSakREF = modell.sisteEvent().saksnummer,
            applikasjonBehandlingREF = modell.sisteEvent().eksternId.toString(),
            styringsinformasjonListe = listOf(),
            avslutningsstatus = BehandlingAvsluttet.Avslutningsstatus("", "", "")
        )
        sakOgBehadlingProducer.avsluttetBehandling(behandlingAvsluttet)

    }

    private fun gregDate(localDate: LocalDate): XMLGregorianCalendar? {
        val gcal = GregorianCalendar.from(localDate.atStartOfDay(ZoneId.systemDefault()))
        return DatatypeFactory.newInstance().newXMLGregorianCalendar(gcal)
    }
}