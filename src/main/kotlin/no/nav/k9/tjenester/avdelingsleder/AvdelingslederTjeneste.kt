package no.nav.k9.tjenester.avdelingsleder

import no.nav.k9.domene.lager.oppgave.*
import no.nav.k9.domene.modell.AndreKriterierType
import no.nav.k9.domene.modell.BehandlingType
import java.time.LocalDate

interface AvdelingslederTjeneste {
    fun hentOppgaveFiltreringer(avdelingsEnhet: String): List<OppgaveKø>

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

    fun hentAvdelinger(): List<Avdeling>

    fun settSorteringTidsintervallDato(oppgaveFiltreringId: Long?, fomDato: LocalDate, tomDato: LocalDate)

    fun settSorteringNumeriskIntervall(oppgaveFiltreringId: Long?, fra: Long?, til: Long?)

    fun settSorteringTidsintervallValg(oppgaveFiltreringId: Long?, erDynamiskPeriode: Boolean)
}