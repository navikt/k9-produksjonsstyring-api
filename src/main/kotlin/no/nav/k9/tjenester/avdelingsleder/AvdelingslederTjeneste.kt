package no.nav.k9.tjenester.avdelingsleder

import no.nav.k9.domene.lager.oppgave.Reservasjon
import no.nav.k9.domene.modell.Enhet
import no.nav.k9.domene.modell.KøSortering
import no.nav.k9.domene.modell.OppgaveKø
import no.nav.k9.domene.modell.Saksbehandler
import no.nav.k9.domene.repository.OppgaveKøRepository
import no.nav.k9.domene.repository.OppgaveRepository
import no.nav.k9.domene.repository.ReservasjonRepository
import no.nav.k9.domene.repository.SaksbehandlerRepository
import no.nav.k9.integrasjon.azuregraph.AzureGraphService
import no.nav.k9.tjenester.avdelingsleder.oppgaveko.*
import no.nav.k9.tjenester.avdelingsleder.reservasjoner.ReservasjonDto
import no.nav.k9.tjenester.saksbehandler.oppgave.OppgaveTjeneste
import no.nav.k9.tjenester.saksbehandler.saksliste.OppgavekøDto
import no.nav.k9.tjenester.saksbehandler.saksliste.SorteringDto
import java.time.LocalDate
import java.util.*

class AvdelingslederTjeneste(
    private val oppgaveKøRepository: OppgaveKøRepository,
    private val saksbehandlerRepository: SaksbehandlerRepository,
    private val oppgaveTjeneste: OppgaveTjeneste,
    private val reservasjonRepository: ReservasjonRepository,
    private val oppgaveRepository: OppgaveRepository
) {

    suspend fun hentOppgaveKøer(): List<OppgavekøDto> {
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
                skjermet = it.skjermet,
                antallBehandlinger = oppgaveTjeneste.hentAntallOppgaver(it.id, true),
                saksbehandlere = it.saksbehandlere
            )
        }
    }

    suspend fun opprettOppgaveKø(): OppgavekøIdDto {
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
                fomDato = null,
                tomDato = null,
                saksbehandlere = mutableListOf()
            )
        }
        oppgaveKøRepository.oppdaterKøMedOppgaver(uuid)
        return OppgavekøIdDto(uuid.toString())
    }

    fun slettOppgavekø(uuid: UUID) {
        oppgaveKøRepository.slett(uuid)
    }

    fun søkSaksbehandler(epostDto: EpostDto): Saksbehandler {
        var saksbehandler = saksbehandlerRepository.finnSaksbehandlerMedEpost(epostDto.epost)
        if (saksbehandler == null) {
            saksbehandler = Saksbehandler(
                null, null, epostDto.epost
            )
            saksbehandlerRepository.addSaksbehandler(saksbehandler)
        }
        return saksbehandler
    }

    suspend fun fjernSaksbehandler(epost: String) {
        saksbehandlerRepository.slettSaksbehandler(epost)
        oppgaveKøRepository.hent().forEach { t: OppgaveKø ->
            oppgaveKøRepository.lagre(t.id) { oppgaveKø ->
                oppgaveKø!!.saksbehandlere =
                    oppgaveKø.saksbehandlere.filter { saksbehandlerRepository.finnSaksbehandlerMedEpost(it.epost) != null }
                        .toMutableList()
                oppgaveKø
            }
        }
    }

    fun hentSaksbehandlere(): List<Saksbehandler> {
        return saksbehandlerRepository.hentAlleSaksbehandlere()
    }

    suspend  fun endreBehandlingsType(behandling: BehandlingsTypeDto) {
        oppgaveKøRepository.lagre(UUID.fromString(behandling.id)) { oppgaveKø ->
            if (behandling.checked) oppgaveKø!!.filtreringBehandlingTyper.add(behandling.behandlingType)
            else oppgaveKø!!.filtreringBehandlingTyper = oppgaveKø.filtreringBehandlingTyper.filter {
                it != behandling.behandlingType
            }.toMutableList()
            oppgaveKø
        }
        oppgaveKøRepository.oppdaterKøMedOppgaver(UUID.fromString(behandling.id))
    }

    suspend fun endreSkjerming(skjermet: SkjermetDto) {
        oppgaveKøRepository.lagre(UUID.fromString(skjermet.id)) { oppgaveKø ->
            oppgaveKø!!.skjermet = skjermet.skjermet
            oppgaveKø
        }
        oppgaveKøRepository.oppdaterKøMedOppgaver(UUID.fromString(skjermet.id))
    }

    suspend  fun endreYtelsesType(ytelse: YtelsesTypeDto) {
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

    suspend  fun endreKriterium(kriteriumDto: AndreKriterierDto) {
        oppgaveKøRepository.lagre(UUID.fromString(kriteriumDto.id))
        { oppgaveKø ->
            if (kriteriumDto.checked) {
                oppgaveKø!!.filtreringAndreKriterierType = oppgaveKø.filtreringAndreKriterierType.filter {
                    it.andreKriterierType != kriteriumDto.andreKriterierType
                }.toMutableList()
                oppgaveKø.filtreringAndreKriterierType.add(kriteriumDto)
            } else oppgaveKø!!.filtreringAndreKriterierType = oppgaveKø.filtreringAndreKriterierType.filter {
                it.andreKriterierType != kriteriumDto.andreKriterierType
            }.toMutableList()
            oppgaveKø
        }
        oppgaveKøRepository.oppdaterKøMedOppgaver(UUID.fromString(kriteriumDto.id))
    }

    suspend  fun endreOppgavekøNavn(køNavn: OppgavekøNavnDto) {
        oppgaveKøRepository.lagre(UUID.fromString(køNavn.id)) { oppgaveKø ->
            oppgaveKø!!.navn = køNavn.navn
            oppgaveKø
        }
    }

    suspend fun endreKøSortering(køSortering: KøSorteringDto) {
        oppgaveKøRepository.lagre(UUID.fromString(køSortering.id)) { oppgaveKø ->
            oppgaveKø!!.sortering = køSortering.oppgavekoSorteringValg
            oppgaveKø
        }
        oppgaveKøRepository.oppdaterKøMedOppgaver(UUID.fromString(køSortering.id))

    }

    suspend  fun endreKøSorteringDato(datoSortering: SorteringDatoDto) {
        oppgaveKøRepository.lagre(UUID.fromString(datoSortering.id)) { oppgaveKø ->
            oppgaveKø!!.fomDato = datoSortering.fomDato
            oppgaveKø.tomDato = datoSortering.tomDato
            oppgaveKø
        }
        oppgaveKøRepository.oppdaterKøMedOppgaver(UUID.fromString(datoSortering.id))
    }

    suspend fun leggFjernSaksbehandlerOppgavekø(saksbehandlerKø: SaksbehandlerOppgavekoDto) {
        val saksbehandler = saksbehandlerRepository.finnSaksbehandlerMedEpost(
            saksbehandlerKø.epost
        )!!
        oppgaveKøRepository.lagre(UUID.fromString(saksbehandlerKø.id))
        { oppgaveKø ->
            if (saksbehandlerKø.checked && !oppgaveKø!!.saksbehandlere.any { it.epost == saksbehandler.epost }) {
                oppgaveKø.saksbehandlere.add(
                    saksbehandler
                )
            } else oppgaveKø!!.saksbehandlere = oppgaveKø.saksbehandlere.filter {
                it.epost != saksbehandlerKø.epost
            }.toMutableList()
            oppgaveKø
        }
    }

    suspend fun hentAlleReservasjoner(): List<ReservasjonDto> {
        return reservasjonRepository.hent(oppgaveKøRepository,  oppgaveRepository).map {
            val oppgave = oppgaveRepository.hent(it.oppgave)
            val saksbehandler = saksbehandlerRepository.finnSaksbehandlerMedIdent(it.reservertAv)
            ReservasjonDto(
                  it.reservertAv,
                  if (saksbehandler != null) saksbehandler.navn!! else "",
                  it.reservertTil!!,
                  it.oppgave,
                  oppgave.fagsakSaksnummer,
                  oppgave.behandlingType
            )
        }
    }
}
