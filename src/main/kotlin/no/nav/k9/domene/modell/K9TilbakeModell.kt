package no.nav.k9.domene.modell

import io.ktor.util.*
import no.nav.k9.domene.lager.oppgave.Oppgave
import no.nav.k9.domene.repository.ReservasjonRepository
import no.nav.k9.domene.repository.SaksbehandlerRepository
import no.nav.k9.integrasjon.kafka.dto.BehandlingProsessEventDto
import no.nav.k9.integrasjon.kafka.dto.BehandlingProsessEventTilbakeDto
import no.nav.k9.integrasjon.kafka.dto.EventHendelse
import no.nav.k9.integrasjon.sakogbehandling.kontrakt.BehandlingAvsluttet
import no.nav.k9.integrasjon.sakogbehandling.kontrakt.BehandlingOpprettet
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon
import no.nav.k9.statistikk.kontrakter.Aktør
import no.nav.k9.statistikk.kontrakter.Behandling
import no.nav.k9.statistikk.kontrakter.Sak
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.*

data class K9TilbakeModell(
    val eventer: List<BehandlingProsessEventDto>
) : IModell{
    private val `Omsorgspenger, Pleiepenger og opplæringspenger` = "ab0271"
    override fun oppgave(sisteEvent: BehandlingProsessEventDto): Oppgave {
        val event = sisteEvent
        val eventResultat = sisteEvent.aktiveAksjonspunkt().eventResultatTilbake()

        var aktiv = true
        var oppgaveAvsluttet: LocalDateTime? = null
        var beslutterOppgave = false
        var registrerPapir = false

        when (eventResultat) {
            EventResultat.LUKK_OPPGAVE -> {
                aktiv = false
                oppgaveAvsluttet = event.eventTid
            }
            EventResultat.LUKK_OPPGAVE_VENT -> {
                aktiv = false
                oppgaveAvsluttet = event.eventTid
            }
            EventResultat.LUKK_OPPGAVE_MANUELT_VENT -> {
                aktiv = false
                oppgaveAvsluttet = event.eventTid
            }
            EventResultat.GJENÅPNE_OPPGAVE -> TODO()
            EventResultat.OPPRETT_BESLUTTER_OPPGAVE -> {
                beslutterOppgave = true
            }
            EventResultat.OPPRETT_PAPIRSØKNAD_OPPGAVE -> {
                registrerPapir = true
            }
            EventResultat.OPPRETT_OPPGAVE -> {
                aktiv = true
            }
        }
        if (event.eventHendelse == EventHendelse.AKSJONSPUNKT_AVBRUTT || event.eventHendelse == EventHendelse.AKSJONSPUNKT_UTFØRT) {
            aktiv = false
        }
        if (FagsakYtelseType.fraKode(event.ytelseTypeKode) == FagsakYtelseType.FRISINN) {
            aktiv = false
        }
        var behandlingStatus = event.behandlingStatus
        // feil i dto, sjekker begge feltene
        behandlingStatus = behandlingStatus ?: event.behandlinStatus ?: BehandlingStatus.OPPRETTET.kode
        if (behandlingStatus == BehandlingStatus.AVSLUTTET.kode) {
            aktiv = false
        }
        return Oppgave(
            behandlingId = event.behandlingId,
            fagsakSaksnummer = event.saksnummer,
            aktorId = event.aktørId,
            behandlendeEnhet = event.behandlendeEnhet ?: "",
            behandlingType = BehandlingType.fraKode(event.behandlingTypeKode),
            fagsakYtelseType = FagsakYtelseType.fraKode(event.ytelseTypeKode),
            aktiv = aktiv,
            forsteStonadsdag = event.eventTid.toLocalDate(),
            utfortFraAdmin = false,
            behandlingsfrist = event.eventTid.plusDays(21),
            behandlingStatus = BehandlingStatus.fraKode(behandlingStatus),
            eksternId = event.eksternId ?: UUID.randomUUID(),
            behandlingOpprettet = event.opprettetBehandling,
            oppgaveAvsluttet = oppgaveAvsluttet,
            system = event.fagsystem.name,
            oppgaveEgenskap = emptyList(),
            aksjonspunkter = event.aktiveAksjonspunkt(),
            utenlands = false,
            tilBeslutter = beslutterOppgave,
            kombinert = false,
            registrerPapir = registrerPapir,
            selvstendigFrilans = false,
            søktGradering = false,
            utbetalingTilBruker = false,
            kode6 = false,
            årskvantum = false,
            avklarMedlemskap = false,
            vurderopptjeningsvilkåret = false,
            eventTid = event.eventTid,
            ansvarligSaksbehandlerForTotrinn = event.ansvarligSaksbehandlerForTotrinn,
            ansvarligSaksbehandlerIdent = event.ansvarligSaksbehandlerIdent
        )
    }
    @KtorExperimentalAPI
    fun behandlingOpprettetSakOgBehandling(

    ): BehandlingOpprettet {
        val sisteEvent = sisteEvent()
        val behandlingOpprettet = BehandlingOpprettet(
            hendelseType = "behandlingOpprettet",
            hendelsesId = sisteEvent.eksternId.toString() + "_" + eventer.size,
            hendelsesprodusentREF = BehandlingOpprettet.HendelsesprodusentREF("", "", "FS39"),
            hendelsesTidspunkt = sisteEvent.eventTid,
            behandlingsID = ("k9-los-" + sisteEvent.behandlingId),
            behandlingstype = BehandlingOpprettet.Behandlingstype(
                "",
                "",
                BehandlingType.fraKode(sisteEvent.behandlingTypeKode).kodeverk
            ),
            sakstema = BehandlingOpprettet.Sakstema("", "", "OMS"),
            behandlingstema = BehandlingOpprettet.Behandlingstema(
                `Omsorgspenger, Pleiepenger og opplæringspenger`,
                `Omsorgspenger, Pleiepenger og opplæringspenger`,
                `Omsorgspenger, Pleiepenger og opplæringspenger`
            ),
            aktoerREF = listOf(BehandlingOpprettet.AktoerREF(sisteEvent.aktørId)),
            ansvarligEnhetREF = "NASJONAL",
            primaerBehandlingREF = null,
            sekundaerBehandlingREF = listOf(),
            applikasjonSakREF = sisteEvent().saksnummer,
            applikasjonBehandlingREF = sisteEvent().eksternId.toString().replace("-", ""),
            styringsinformasjonListe = listOf()
        )
        return behandlingOpprettet
    }
    @KtorExperimentalAPI
    fun behandlingAvsluttetSakOgBehandling(
    ): BehandlingAvsluttet {
        val sisteEvent = sisteEvent()
        val behandlingAvsluttet = BehandlingAvsluttet(
            hendelseType = "behandlingAvsluttet",
            hendelsesId = """${sisteEvent.eksternId.toString()}_${eventer.size}""",
            hendelsesprodusentREF = BehandlingAvsluttet.HendelsesprodusentREF("", "", "FS39"),
            hendelsesTidspunkt = sisteEvent.eventTid,
            behandlingsID = ("k9-los-" + sisteEvent.behandlingId),
            behandlingstype = BehandlingAvsluttet.Behandlingstype(
                "",
                "",
                BehandlingType.fraKode(sisteEvent.behandlingTypeKode).kodeverk
            ),
            sakstema = BehandlingAvsluttet.Sakstema("", "", "OMS"),
            behandlingstema = BehandlingAvsluttet.Behandlingstema(
                "ab0149",
                "ab0149",
                `Omsorgspenger, Pleiepenger og opplæringspenger`
            ),
            aktoerREF = listOf(BehandlingAvsluttet.AktoerREF(sisteEvent.aktørId)),
            ansvarligEnhetREF = "NASJONAL",
            primaerBehandlingREF = null,
            sekundaerBehandlingREF = listOf(),
            applikasjonSakREF = sisteEvent().saksnummer,
            applikasjonBehandlingREF = sisteEvent().eksternId.toString().replace("-", ""),
            styringsinformasjonListe = listOf(),
            avslutningsstatus = BehandlingAvsluttet.Avslutningsstatus("", "", "ok")
        )
        return behandlingAvsluttet
    }
    override fun sisteEvent(): BehandlingProsessEventDto {
        return this.eventer[this.eventer.lastIndex]
    }

    override fun forrigeEvent(): BehandlingProsessEventDto? {
        return if (this.eventer.lastIndex > 0) {
            this.eventer[this.eventer.lastIndex - 1]
        } else {
            null
        }
    }

    override fun førsteEvent(): BehandlingProsessEventDto {
        return this.eventer[0]
    }

    override fun starterSak(): Boolean {
        return this.eventer.size == 1
    }

    override fun erTom(): Boolean {
        return this.eventer.isEmpty()
    }
    
    fun bleBeslutter(): Boolean {
        val forrigeEvent = forrigeEvent()
        return forrigeEvent != null && !forrigeEvent.aktiveAksjonspunkt()
            .tilBeslutter() && sisteEvent().aktiveAksjonspunkt().tilBeslutter()
    }

    fun fikkEndretAksjonspunkt(): Boolean {
        val forrigeEvent = forrigeEvent()
        if (forrigeEvent == null) {
            return false
        }

        val forrigeAksjonspunkter = forrigeEvent.aktiveAksjonspunkt()
            .liste
        val nåværendeAksjonspunkter = sisteEvent().aktiveAksjonspunkt().liste
        return forrigeAksjonspunkter != nåværendeAksjonspunkter
    }

    // Array med alle versjoner av modell basert på eventene, brukes når man skal spille av eventer
    override fun alleVersjoner(): MutableList<K9SakModell> {
        val eventListe = mutableListOf<BehandlingProsessEventDto>()
        val modeller = mutableListOf<K9SakModell>()
        for (behandlingProsessEventDto in eventer) {
            eventListe.add(behandlingProsessEventDto)
            modeller.add(K9SakModell(eventListe.toMutableList()))
        }
        return modeller
    }

    override fun dvhSak(): Sak {
        val oppgave = oppgave()
        val zone = ZoneId.of("Europe/Oslo")
        return Sak(
            saksnummer = oppgave.fagsakSaksnummer,
            sakId = oppgave.fagsakSaksnummer,
            funksjonellTid = sisteEvent().eventTid.atOffset(zone.rules.getOffset(sisteEvent().eventTid)),
            tekniskTid = OffsetDateTime.now(),
            opprettetDato = oppgave.behandlingOpprettet.toLocalDate(),
            aktorId = oppgave.aktorId.toLong(),
            aktorer = listOf(Aktør(oppgave.aktorId.toLong(), "Søker", "Søker")),
            ytelseType = oppgave.fagsakYtelseType.navn,
            underType = null,
            sakStatus = oppgave.behandlingStatus.navn,
            ytelseTypeBeskrivelse = null,
            underTypeBeskrivelse = null,
            sakStatusBeskrivelse = null,
            avsender = "K9los",
            versjon = 1
        )
    }
    
    override fun dvhBehandling(
        saksbehandlerRepository: SaksbehandlerRepository,
        reservasjonRepository: ReservasjonRepository
    ): Behandling {
        val oppgave = oppgave()
        val beslutter = if (oppgave.tilBeslutter
            && reservasjonRepository.finnes(oppgave.eksternId) && reservasjonRepository.finnes(oppgave.eksternId)
        ) {
            val saksbehandler =
                saksbehandlerRepository.finnSaksbehandlerMedIdentIkkeTaHensyn(reservasjonRepository.hent(oppgave.eksternId).reservertAv!!)
            saksbehandler?.brukerIdent
        } else {
            ""
        }

        val behandldendeEnhet =
            if (reservasjonRepository.finnes(oppgave.eksternId)) {
                val hentMedHistorikk = reservasjonRepository.hentMedHistorikk(oppgave.eksternId)
                val reservertav = hentMedHistorikk
                    .map { reservasjon -> reservasjon.reservertAv }.first()
                saksbehandlerRepository.finnSaksbehandlerMedIdentIkkeTaHensyn(reservertav)?.enhet?.substringBefore(" ")
            } else {
                "SRV"
            }
        val zone = ZoneId.of("Europe/Oslo")
        return Behandling(
            sakId = oppgave.fagsakSaksnummer,
            behandlingId = oppgave.eksternId.toString(),
            funksjonellTid = sisteEvent().eventTid.atOffset(zone.rules.getOffset(sisteEvent().eventTid)),
            tekniskTid = OffsetDateTime.now(),
            mottattDato = oppgave.behandlingOpprettet.toLocalDate(),
            registrertDato = oppgave.behandlingOpprettet.toLocalDate(),
            vedtaksDato = null,
            relatertBehandlingId = null,
            vedtakId = null,
            saksnummer = oppgave.fagsakSaksnummer,
            behandlingType = oppgave.behandlingType.navn,
            behandlingStatus = oppgave.behandlingStatus.navn,
            resultat = sisteEvent().resultatType,
            resultatBegrunnelse = null,
            utenlandstilsnitt = oppgave.utenlands.toString(),
            behandlingTypeBeskrivelse = null,
            behandlingStatusBeskrivelse = null,
            resultatBeskrivelse = null,
            resultatBegrunnelseBeskrivelse = null,
            utenlandstilsnittBeskrivelse = null,
            beslutter = beslutter,
            saksbehandler = null,
            behandlingOpprettetAv = "system",
            behandlingOpprettetType = null,
            behandlingOpprettetTypeBeskrivelse = null,
            ansvarligEnhetKode = behandldendeEnhet,
            ansvarligEnhetType = "NORG",
            behandlendeEnhetKode = behandldendeEnhet,
            behandlendeEnhetType = "NORG",
            datoForUttak = null,
            datoForUtbetaling = null,
            totrinnsbehandling = oppgave.tilBeslutter,
            avsender = "K9sak",
            versjon = 1
        )
    }
}

fun BehandlingProsessEventTilbakeDto.aktiveAksjonspunkt(): AksjonspunkterTilbake {
    return AksjonspunkterTilbake(this.aksjonspunktKoderMedStatusListe.filter { entry -> entry.value == "OPPR" })
}

data class AksjonspunkterTilbake(val liste: Map<String, String>) {
    fun lengde(): Int {
        return liste.size
    }

    fun påVent(): Boolean {
        return this.liste.map { entry -> AksjonspunktDefinisjon.fraKode(entry.key) }.any { it.erAutopunkt() }
    }

    fun erTom(): Boolean {
        return this.liste.isEmpty()
    }

    fun tilBeslutter(): Boolean {
        return this.liste.map { entry -> AksjonspunktDefinisjon.fraKode(entry.key) }
            .any { it == AksjonspunktDefinisjon.FATTER_VEDTAK }
    }

    fun eventResultat(): EventResultat {
        if (erTom()) {
            return EventResultat.LUKK_OPPGAVE
        }

        if (påVent()) {
            return EventResultat.LUKK_OPPGAVE_VENT
        }

        if (tilBeslutter()) {
            return EventResultat.OPPRETT_BESLUTTER_OPPGAVE
        }

        return EventResultat.OPPRETT_OPPGAVE
    }

    fun eventResultatTilbake(): EventResultat {
        if (erTom()) {
            return EventResultat.LUKK_OPPGAVE
        }

        if (påVentTilbake()) {
            return EventResultat.LUKK_OPPGAVE_VENT
        }

        if (tilBeslutterTilbake()) {
            return EventResultat.OPPRETT_BESLUTTER_OPPGAVE
        }

        return EventResultat.OPPRETT_OPPGAVE
    }

    fun påVentTilbake(): Boolean {
        return this.liste.any {
            when (it.key) {
                "7001", "7002" -> true
                else -> false
            }
        }
    }

    fun tilBeslutterTilbake(): Boolean {
        return this.liste.any {
            when (it.key) {
                "5005" -> true
                else -> false
            }
        }
    }
}

