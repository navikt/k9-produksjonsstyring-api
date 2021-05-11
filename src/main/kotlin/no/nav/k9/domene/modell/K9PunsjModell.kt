package no.nav.k9.domene.modell

import no.nav.k9.domene.lager.oppgave.Oppgave
import no.nav.k9.domene.repository.ReservasjonRepository
import no.nav.k9.domene.repository.SaksbehandlerRepository
import no.nav.k9.integrasjon.kafka.dto.PunsjEventDto
import no.nav.k9.integrasjon.sakogbehandling.kontrakt.BehandlingAvsluttet
import no.nav.k9.integrasjon.sakogbehandling.kontrakt.BehandlingOpprettet
import no.nav.k9.statistikk.kontrakter.Behandling
import no.nav.k9.statistikk.kontrakter.Sak

data class K9PunsjModell(
    val eventer: List<PunsjEventDto>
) : IModell {

    override fun starterSak(): Boolean {
        TODO("Ikke relevant for punsj")
    }

    override fun erTom(): Boolean {
        return this.eventer.isEmpty()
    }

    override fun dvhSak(): Sak {
        TODO("Ikke relevant for punsj")
    }

    override fun dvhBehandling(
        saksbehandlerRepository: SaksbehandlerRepository,
        reservasjonRepository: ReservasjonRepository
    ): Behandling {
        TODO("Ikke relevant for punsj")
    }

    override fun sisteSaksNummer(): String {
        TODO("Ikke relevant for punsj")
    }

    override fun behandlingOpprettetSakOgBehandling(): BehandlingOpprettet {
        TODO("Ikke relevant for punsj")
    }

    override fun behandlingAvsluttetSakOgBehandling(): BehandlingAvsluttet {
        TODO("Ikke relevant for punsj")
    }

    fun forrigeEvent(): PunsjEventDto? {
        return if (this.eventer.lastIndex > 0) {
            this.eventer[this.eventer.lastIndex - 1]
        } else {
            null
        }
    }

    fun sisteEvent(): PunsjEventDto {
        return this.eventer[this.eventer.lastIndex]
    }

    override fun fikkEndretAksjonspunkt(): Boolean {
        val forrigeEvent = forrigeEvent() ?: return false
        val forrigeAksjonspunkter = forrigeEvent.aktiveAksjonspunkt().liste
        val nåværendeAksjonspunkter = sisteEvent().aktiveAksjonspunkt().liste
        return forrigeAksjonspunkter != nåværendeAksjonspunkter
    }

    fun PunsjEventDto.aktiveAksjonspunkt(): Aksjonspunkter {
        return Aksjonspunkter(this.aksjonspunktKoderMedStatusListe.filter { entry -> entry.value == "OPPR" })
    }

    override fun oppgave(): Oppgave {

        val sisteEvent = eventer.last()
        val førsteEvent = eventer.first()

        return Oppgave(
            behandlingId = null,
            fagsakSaksnummer = "",
            journalpostId = førsteEvent.journalpostId.verdi,
            aktorId = førsteEvent.aktørId?.id ?: "",
            behandlendeEnhet = "",
            behandlingsfrist = førsteEvent.eventTid.toLocalDate().plusDays(21).atStartOfDay(),
            behandlingOpprettet = førsteEvent.eventTid,
            forsteStonadsdag = førsteEvent.eventTid.toLocalDate(),
            behandlingStatus = BehandlingStatus.OPPRETTET,
            behandlingType = BehandlingType.FORSTEGANGSSOKNAD,
            fagsakYtelseType = FagsakYtelseType.PPN,
            eventTid = sisteEvent.eventTid,
            aktiv = sisteEvent.aksjonspunktKoderMedStatusListe.any { aksjonspunkt -> aksjonspunkt.value == "OPPR" },
            system = "PUNSJ",
            oppgaveAvsluttet = null,
            utfortFraAdmin = false,
            eksternId = sisteEvent.eksternId,
            oppgaveEgenskap = listOf(),
            aksjonspunkter = sisteEvent.aktiveAksjonspunkt(),
            tilBeslutter = false,
            utbetalingTilBruker = false,
            selvstendigFrilans = false,
            søktGradering = false,
            registrerPapir = true,
            årskvantum = false,
            avklarMedlemskap = false,
            avklarArbeidsforhold = false,
            kode6 = false,
            skjermet = false,
            utenlands = false,
            vurderopptjeningsvilkåret = false,
            ansvarligSaksbehandlerForTotrinn = null,
            ansvarligSaksbehandlerIdent = null
        )
    }
}
