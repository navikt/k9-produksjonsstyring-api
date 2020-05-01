package no.nav.k9.tjenester.avdelingsleder

import no.nav.k9.domene.modell.Enhet
import no.nav.k9.domene.modell.KøSortering
import no.nav.k9.domene.modell.OppgaveKø
import no.nav.k9.domene.repository.OppgaveKøRepository
import no.nav.k9.tjenester.avdelingsleder.oppgaveko.*
import no.nav.k9.tjenester.saksbehandler.oppgave.OppgaveTjeneste
import no.nav.k9.tjenester.saksbehandler.saksliste.OppgavekøDto
import no.nav.k9.tjenester.saksbehandler.saksliste.SorteringDto
import java.time.LocalDate
import java.util.*

class AvdelingslederTjeneste(
    private val oppgaveKøRepository: OppgaveKøRepository,
    private val oppgaveTjeneste: OppgaveTjeneste) {
/*    fun hentOppgaveFiltreringer(avdelingsEnhet: String): List<OppgaveKø>

    fun hentOppgaveFiltering(oppgaveFiltrering: Long?): OppgaveKø

    fun lagNyOppgaveFiltrering(avdelingEnhet: String): Long?

    fun giListeNyttNavn(sakslisteId: Long?, navn: String)

    fun slettOppgaveFiltrering(listeId: Long?)

    fun settSortering(sakslisteId: Long?, sortering: KøSortering)

    fun endreFiltreringBehandlingType(sakslisteId: Long?, behandlingType: BehandlingType, checked: Boolean)

    fun endreFiltreringYtelseType(sakslisteId: Long?, behandlingType: FagsakYtelseType)

    fun endreFiltreringAndreKriterierType(
        sakslisteId: Long?,
        behandlingType: AndreKriterierType,
        checked: Boolean,
        inkluder: Boolean
    )

    fun leggSaksbehandlerTilListe(oppgaveFiltreringId: Long?, saksbehandlerIdent: String)

    fun fjernSaksbehandlerFraListe(oppgaveFiltreringId: Long?, saksbehandlerIdent: String)


    fun settSorteringTidsintervallDato(oppgaveFiltreringId: Long?, fomDato: LocalDate, tomDato: LocalDate)

    fun settSorteringNumeriskIntervall(oppgaveFiltreringId: Long?, fra: Long?, til: Long?)

    fun settSorteringTidsintervallValg(oppgaveFiltreringId: Long?, erDynamiskPeriode: Boolean) */

    fun hentOppgaveKøer(): List<OppgavekøDto> {
        return oppgaveKøRepository.hent().map {
            OppgavekøDto(
                id = it.id,
                navn = it.navn,
                sortering = SorteringDto(
                    sorteringType = KøSortering.fraKode(it.sortering.kode),
                    fomDato = it.fomDato,
                    tomDato = it.tomDato
                ),
                behandlingTyper = it.filtreringBehandlingTyper,
                fagsakYtelseTyper = it.filtreringYtelseTyper,
                andreKriterier = it.filtreringAndreKriterierType,
                sistEndret = it.sistEndret,
                antallBehandlinger = oppgaveTjeneste.hentAntallOppgaver(it.id),
                saksbehandlere = it.saksbehandlere
            )
        }
    }

    fun opprettOppgaveKø(): OppgavekøIdDto {
        val uuid = UUID.randomUUID()
        oppgaveKøRepository.lagre(
            OppgaveKø(
                uuid,
                "Ny kø",
                LocalDate.now(),
                KøSortering.OPPRETT_BEHANDLING,
                mutableListOf(),
                mutableListOf(),
                mutableListOf(),
                Enhet.NASJONAL,
                LocalDate.now(),
                LocalDate.now(),
                emptyList()
            )
        )
        return OppgavekøIdDto(uuid.toString())
    }

    fun slettOppgavekø(uuid: UUID) {
        oppgaveKøRepository.slett(uuid)
    }

    fun opprettSaksbehandler(ident: String) {

    }

    fun endreBehandlingsType(behandling: BehandlingsTypeDto) {
        val oppgaveKø = oppgaveKøRepository.hentOppgavekø(UUID.fromString(behandling.id))
        if (behandling.checked) oppgaveKø.filtreringBehandlingTyper.add(behandling.behandlingType)
        else oppgaveKø.filtreringBehandlingTyper = oppgaveKø.filtreringBehandlingTyper.filter {
            it != behandling.behandlingType }.toMutableList()
        oppgaveKøRepository.lagre(oppgaveKø)
    }

    fun endreYtelsesType(ytelse: YtelsesTypeDto) {
        val oppgaveKø = oppgaveKøRepository.hentOppgavekø(UUID.fromString(ytelse.id))
        oppgaveKø.filtreringYtelseTyper = mutableListOf()
        if (ytelse.fagsakYtelseType !== null) {
            oppgaveKø.filtreringYtelseTyper.add(ytelse.fagsakYtelseType)
        }
        oppgaveKøRepository.lagre(oppgaveKø)
    }

    fun endreKriterium(kriteriumDto: AndreKriterierDto) {
        val oppgaveKø = oppgaveKøRepository.hentOppgavekø(UUID.fromString(kriteriumDto.id))
        if (kriteriumDto.checked) oppgaveKø.filtreringAndreKriterierType.add(kriteriumDto.andreKriterierType)
        else oppgaveKø.filtreringAndreKriterierType = oppgaveKø.filtreringAndreKriterierType.filter {
            it != kriteriumDto.andreKriterierType }.toMutableList()
        oppgaveKøRepository.lagre(oppgaveKø)
    }

    fun endreOppgavekøNavn(køNavn: OppgavekøNavnDto) {
        val oppgaveKø = oppgaveKøRepository.hentOppgavekø(UUID.fromString(køNavn.id))
        oppgaveKø.navn = køNavn.navn
        oppgaveKøRepository.lagre(oppgaveKø)
    }

    fun endreKøSortering(køSortering: KøSorteringDto) {
        val oppgaveKø = oppgaveKøRepository.hentOppgavekø(UUID.fromString(køSortering.id))
        oppgaveKø.sortering = køSortering.oppgavekoSorteringValg
        oppgaveKøRepository.lagre(oppgaveKø)
    }

    fun endreKøSorteringDato(datoSortering: SorteringDatoDto) {
        val oppgaveKø = oppgaveKøRepository.hentOppgavekø(UUID.fromString(datoSortering.id))
        oppgaveKø.fomDato = datoSortering.fomDato
        oppgaveKø.fomDato = datoSortering.fomDato
        oppgaveKøRepository.lagre(oppgaveKø)
    }
}
