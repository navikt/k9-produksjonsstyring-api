package no.nav.k9.tjenester.avdelingsleder

import no.nav.k9.domene.lager.oppgave.*
import no.nav.k9.domene.modell.*
import no.nav.k9.domene.repository.OppgaveKøRepository
import no.nav.k9.domene.repository.OppgaveRepository
import no.nav.k9.tjenester.saksbehandler.oppgave.OppgaveTjeneste
import no.nav.k9.tjenester.saksbehandler.saksliste.OppgavekøDto
import no.nav.k9.tjenester.saksbehandler.saksliste.SorteringDto
import java.time.LocalDate

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
    fun opprettOppgaveKø(oppgaveKø: OppgaveKø) {
        oppgaveKø.sistEndret = LocalDate.now()
        oppgaveKøRepository.lagre(oppgaveKø)
    }
}
