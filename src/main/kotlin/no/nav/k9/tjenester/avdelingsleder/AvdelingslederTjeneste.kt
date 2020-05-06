package no.nav.k9.tjenester.avdelingsleder

import io.ktor.util.KtorExperimentalAPI
import no.nav.k9.domene.modell.Enhet
import no.nav.k9.domene.modell.KøSortering
import no.nav.k9.domene.modell.OppgaveKø
import no.nav.k9.domene.modell.Saksbehandler
import no.nav.k9.domene.repository.OppgaveKøRepository
import no.nav.k9.domene.repository.SaksbehandlerRepository
import no.nav.k9.integrasjon.azuregraph.AzureGraphService
import no.nav.k9.tjenester.avdelingsleder.oppgaveko.*
import no.nav.k9.tjenester.saksbehandler.oppgave.OppgaveTjeneste
import no.nav.k9.tjenester.saksbehandler.saksliste.OppgavekøDto
import no.nav.k9.tjenester.saksbehandler.saksliste.SorteringDto
import java.time.LocalDate
import java.util.*

class AvdelingslederTjeneste(
    private val oppgaveKøRepository: OppgaveKøRepository,
    private val saksbehandlerRepository: SaksbehandlerRepository,
    private val azureGraphService: AzureGraphService,
    private val oppgaveTjeneste: OppgaveTjeneste
) {

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
                antallBehandlinger = it.oppgaver.size,
                saksbehandlere = it.saksbehandlere
            )
        }
    }

    fun opprettOppgaveKø(): OppgavekøIdDto {
        val uuid = UUID.randomUUID()
        oppgaveKøRepository.lagre(uuid) {
            OppgaveKø(
                id = uuid,
                navn = "Ny kø",
                sistEndret = LocalDate.now(),
                sortering = KøSortering.OPPRETT_BEHANDLING,
                filtreringBehandlingTyper = mutableListOf(),
                filtreringYtelseTyper = mutableListOf(),
                filtreringAndreKriterierType = mutableListOf(),
                enhet = Enhet.NASJONAL,
                fomDato = LocalDate.now(),
                tomDato = LocalDate.now(),
                saksbehandlere = emptyList()
            )
        }
        oppgaveKøRepository.oppdaterKøMedOppgaver(uuid)
        return OppgavekøIdDto(uuid.toString())
    }

    fun slettOppgavekø(uuid: UUID) {
        oppgaveKøRepository.slett(uuid)
    }

    fun søkSaksbehandler(epostDto: EpostDto): Saksbehandler {
        var saksbehandler = saksbehandlerRepository.finnSaksbehandler(epostDto.epost)
        if (saksbehandler == null) {
            saksbehandler = Saksbehandler(
                null, null, epostDto.epost)
            saksbehandlerRepository.addSaksbehandler(saksbehandler)
        }
        return saksbehandler
    }

    fun hentSaksbehandlere(): List<Saksbehandler> {
        return saksbehandlerRepository.hentAlleSaksbehandlere()
    }

    fun endreBehandlingsType(behandling: BehandlingsTypeDto) {
        oppgaveKøRepository.lagre(UUID.fromString(behandling.id)) { oppgaveKø ->
            if (behandling.checked) oppgaveKø!!.filtreringBehandlingTyper.add(behandling.behandlingType)
            else oppgaveKø!!.filtreringBehandlingTyper = oppgaveKø.filtreringBehandlingTyper.filter {
                it != behandling.behandlingType
            }.toMutableList()
            oppgaveKø
        }

        oppgaveKøRepository.oppdaterKøMedOppgaver(UUID.fromString(behandling.id))

    }

    fun endreYtelsesType(ytelse: YtelsesTypeDto) {
        oppgaveKøRepository.lagre(UUID.fromString(ytelse.id))
        { oppgaveKø ->
            oppgaveKø!!.filtreringYtelseTyper = mutableListOf()
            if (ytelse.fagsakYtelseType != null) {
                oppgaveKø.filtreringYtelseTyper.add(ytelse.fagsakYtelseType)
            }
            oppgaveKø
        }
        oppgaveKøRepository.oppdaterKøMedOppgaver(UUID.fromString(ytelse.id))

    }

    fun endreKriterium(kriteriumDto: AndreKriterierDto) {
        oppgaveKøRepository.lagre(UUID.fromString(kriteriumDto.id))
        { oppgaveKø ->
            if (kriteriumDto.checked) oppgaveKø!!.filtreringAndreKriterierType.add(kriteriumDto.andreKriterierType)
            else oppgaveKø!!.filtreringAndreKriterierType = oppgaveKø.filtreringAndreKriterierType.filter {
                it != kriteriumDto.andreKriterierType
            }.toMutableList()
            oppgaveKø
        }
        oppgaveKøRepository.oppdaterKøMedOppgaver(UUID.fromString(kriteriumDto.id))

    }

    fun endreOppgavekøNavn(køNavn: OppgavekøNavnDto) {
        oppgaveKøRepository.lagre(UUID.fromString(køNavn.id)) { oppgaveKø ->
            oppgaveKø!!.navn = køNavn.navn
            oppgaveKø
        }
    }

    fun endreKøSortering(køSortering: KøSorteringDto) {
        oppgaveKøRepository.lagre(UUID.fromString(køSortering.id)) { oppgaveKø ->
            oppgaveKø!!.sortering = køSortering.oppgavekoSorteringValg
            oppgaveKø
        }
        oppgaveKøRepository.oppdaterKøMedOppgaver(UUID.fromString(køSortering.id))

    }

    fun endreKøSorteringDato(datoSortering: SorteringDatoDto) {
        oppgaveKøRepository.lagre(UUID.fromString(datoSortering.id)) { oppgaveKø ->
            oppgaveKø!!.fomDato = datoSortering.fomDato
            oppgaveKø.fomDato = datoSortering.fomDato
            oppgaveKø
        }
        oppgaveKøRepository.oppdaterKøMedOppgaver(UUID.fromString(datoSortering.id))
    }
}
