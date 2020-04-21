package no.nav.k9.tjenester.avdelingsleder

import no.nav.k9.domene.modell.Enhet
import no.nav.k9.domene.modell.KøSortering
import no.nav.k9.domene.modell.OppgaveKø
import no.nav.k9.domene.modell.Saksbehandler
import no.nav.k9.domene.repository.OppgaveKøRepository
import no.nav.k9.tjenester.avdelingsleder.oppgaveko.BehandlingsTypeDto
import no.nav.k9.tjenester.avdelingsleder.oppgaveko.OppgavekøIdDto
import no.nav.k9.tjenester.avdelingsleder.oppgaveko.YtelsesTypeDto
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
                    sorteringType = KøSortering.fraKode(it.sortering.navn),
                    fomDato = it.fomDato,
                    tomDato = it.tomDato,
                    erDynamiskPeriode = it.erDynamiskPeriode
                ),
                behandlingTyper = it.filtreringBehandlingTyper,
                fagsakYtelseTyper = it.filtreringYtelseTyper,
                andreKriterierType = listOf(),
                sistEndret = it.sistEndret,
                antallBehandlinger = oppgaveTjeneste.hentAntallOppgaver(it.id),
                tilBeslutter = it.tilBeslutter,
                utbetalingTilBruker = it.utbetalingTilBruker,
                selvstendigFrilans = it.selvstendigFrilans,
                kombinert = it.kombinert,
                søktGradering = it.søktGradering,
                registrerPapir = it.registrerPapir,
                saksbehandlere = it.saksbehandlere
            )
        }
    }
    fun opprettOppgaveKø(): OppgavekøIdDto {
        val uuid = UUID.randomUUID()
        oppgaveKøRepository.lagre(
            oppgaveKø = OppgaveKø(
                id = uuid,
                navn = "Oppgavekø",
                sistEndret = LocalDate.now(),
                sortering = KøSortering.OPPRETT_BEHANDLING,
                filtreringBehandlingTyper = mutableListOf(),
                filtreringYtelseTyper = mutableListOf(),
                enhet = Enhet.NASJONAL,
                erDynamiskPeriode = false,
                fomDato = LocalDate.now(),
                tomDato = LocalDate.now(),
                saksbehandlere = listOf(Saksbehandler("alexaban", "Sara Saksbehandler")),
                tilBeslutter = false,
                utbetalingTilBruker = false,
                selvstendigFrilans = false,
                kombinert = false,
                søktGradering = false,
                registrerPapir = false
            )
        )
        return OppgavekøIdDto(uuid)
    }

    fun slettOppgavekø(uuid: UUID) {
        oppgaveKøRepository.slett(uuid)
    }

    fun endreBehandlingsType(behandling: BehandlingsTypeDto) {
        val oppgaveKø = oppgaveKøRepository.hentOppgavekø(behandling.oppgavekoId.id)
        if (behandling.markert) oppgaveKø.filtreringBehandlingTyper.add(behandling.behandlingType)
        else oppgaveKø.filtreringBehandlingTyper = oppgaveKø.filtreringBehandlingTyper.filter {
            it != behandling.behandlingType }.toMutableList()
        oppgaveKøRepository.lagre(oppgaveKø)
    }

    fun endreYtelsesType(ytelse: YtelsesTypeDto) {
        val oppgaveKø = oppgaveKøRepository.hentOppgavekø(ytelse.oppgavekoId.id)
        if (ytelse.markert) oppgaveKø.filtreringYtelseTyper.add(ytelse.ytelseType)
        else oppgaveKø.filtreringYtelseTyper = oppgaveKø.filtreringYtelseTyper.filter {
            it != ytelse.ytelseType }.toMutableList()
        oppgaveKøRepository.lagre(oppgaveKø)
    }
}
