package no.nav.k9.tjenester.avdelingsleder

import no.nav.k9.domene.lager.oppgave.*
import no.nav.k9.domene.modell.*
import no.nav.k9.domene.repository.OppgaveKøRepository
import no.nav.k9.domene.repository.OppgaveRepository
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
                it.id,
                it.navn,
                SorteringDto(
                    KøSortering.fraKode(it.sortering.navn),
                    it.fomDato,
                    it.tomDato,
                    it.erDynamiskPeriode),
                it.filtreringBehandlingTyper,
                it.filtreringYtelseTyper,
                it.sistEndret,
                oppgaveTjeneste.hentAntallOppgaver(it.id),
                it.tilBeslutter,
                it.utbetalingTilBruker,
                it.selvstendigFrilans,
                it.kombinert,
                it.søktGradering,
                it.registrerPapir,
                it.saksbehandlere
            )
        }
    }
    fun opprettOppgaveKø(): OppgavekøIdDto {
        val uuid = UUID.randomUUID()
        oppgaveKøRepository.lagre(
            OppgaveKø(
            uuid,
            "",
            LocalDate.now(),
            KøSortering.OPPRETT_BEHANDLING,
            mutableListOf(),
            mutableListOf(),
            Enhet.NASJONAL,
            false,
            LocalDate.now(),
            LocalDate.now(),
            emptyList(),
            false,
            false,
            false,
            false,
            false,
            false
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
