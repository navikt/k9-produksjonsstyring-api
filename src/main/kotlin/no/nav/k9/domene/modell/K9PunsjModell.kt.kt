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
)  : IModell {

    override fun starterSak(): Boolean {
        TODO("Not yet implemented")
    }

    override fun erTom(): Boolean {
        TODO("Not yet implemented")
    }

    override fun dvhSak(): Sak {
        TODO("Not yet implemented")
    }

    override fun dvhBehandling(
        saksbehandlerRepository: SaksbehandlerRepository,
        reservasjonRepository: ReservasjonRepository
    ): Behandling {
        TODO("Not yet implemented")
    }

    override fun sisteSaksNummer(): String {
        TODO("Not yet implemented")
    }

    override fun behandlingOpprettetSakOgBehandling(): BehandlingOpprettet {
        TODO("Not yet implemented")
    }

    override fun behandlingAvsluttetSakOgBehandling(): BehandlingAvsluttet {
        TODO("Not yet implemented")
    }

    fun oppgave(): Oppgave {

        val sisteEvent = eventer.last()
        val førsteEvent = eventer.first()
        val aksjonspunkter = mutableMapOf<String, String>()
        for (pair in sisteEvent.aksjonspunkter.map { it.kode to it.navn }) {
            aksjonspunkter[pair.first] = pair.second
        }
        
        return Oppgave(
           behandlingId = null,
           fagsakSaksnummer = "",
           journalpostId = førsteEvent.journalpostId.verdi,
           aktorId = førsteEvent.aktørId?.id?:"",
           behandlendeEnhet = "",
           behandlingsfrist = førsteEvent.eventTid.toLocalDate().plusDays(21).atStartOfDay(),
           behandlingOpprettet = førsteEvent.eventTid,
           forsteStonadsdag = førsteEvent.eventTid.toLocalDate(),
           behandlingStatus = BehandlingStatus.OPPRETTET,
           behandlingType = BehandlingType.FORSTEGANGSSOKNAD,
           fagsakYtelseType = FagsakYtelseType.PPN,
           eventTid = sisteEvent.eventTid,
           aktiv = sisteEvent.aksjonspunkter.any{ it.kode == "OPPR"},
           system = "PUNSJ",
           oppgaveAvsluttet = null,
           utfortFraAdmin = false,
           eksternId = sisteEvent.eksternId,
           oppgaveEgenskap = listOf(),
           aksjonspunkter = Aksjonspunkter(liste = aksjonspunkter.toMap()),
           tilBeslutter = false,
           utbetalingTilBruker = false,
           selvstendigFrilans = false,
           kombinert = false,
           søktGradering = false,
           registrerPapir = true,
           årskvantum = false,
           avklarMedlemskap = false,
           kode6 = false,
           skjermet = false,
           utenlands = false,
           vurderopptjeningsvilkåret = false,
           ansvarligSaksbehandlerForTotrinn = null,
           ansvarligSaksbehandlerIdent = null
       )
    }
}
