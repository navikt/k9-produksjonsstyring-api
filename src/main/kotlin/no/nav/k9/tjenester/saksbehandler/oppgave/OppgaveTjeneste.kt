package no.nav.k9.tjenester.saksbehandler.oppgave

import no.nav.k9.domene.lager.aktør.TpsPersonDto
import no.nav.k9.domene.lager.oppgave.Oppgave
import no.nav.k9.domene.lager.oppgave.OppgaveKø
import no.nav.k9.domene.lager.oppgave.Reservasjon
import java.util.*

interface OppgaveTjeneste {
    fun hentOppgaver(sakslisteId: Long): List<Oppgave>
    fun hentNesteOppgaver(sakslisteId: Long): List<Oppgave>
    fun hentOppgaverForSaksnummer(fagsakSaksnummer: Long): List<Oppgave>
    fun hentAktiveOppgaverForSaksnummer(fagsakSaksnummerListe: Collection<Long>): List<Oppgave>
    fun hentReservasjonerTilknyttetAktiveOppgaver(): List<Reservasjon>
    fun reserverOppgave(oppgaveId: Long): Reservasjon
    fun hentReservasjon(oppgaveId: Long): Reservasjon
    fun frigiOppgave(oppgaveId: Long, begrunnelse: String): Reservasjon
    fun forlengReservasjonPåOppgave(oppgaveId: Long): Reservasjon
    fun flyttReservasjon(
        oppgaveId: Long,
        brukernavn: String,
        begrunnelse: String
    ): Reservasjon

    fun hentAlleOppgaveFiltrering(brukerIdent: String): List<OppgaveKø>
    fun hentOppgaveFiltreringerForPåloggetBruker(): List<OppgaveKø>
    fun hentPersonInfo(aktørId: Long): TpsPersonDto
    fun hentPersonInfoOptional(aktørId: Long): Optional<TpsPersonDto>
    fun hentAntallOppgaver(behandlingsKø: Long, forAvdelingsleder: Boolean): Int
    fun hentAntallOppgaverForAvdeling(avdelingEnhet: String): Int
    fun harForandretOppgaver(oppgaveIder: List<Long>): Boolean
    fun hentSakslistensSaksbehandlere(sakslisteId: Long): List<SaksbehandlerinformasjonDto>
    fun hentSisteReserverteOppgaver(): List<Oppgave>
    fun hentSaksbehandlerNavnOgAvdelinger(ident: String): SaksbehandlerinformasjonDto
    fun hentNavnHvisReservertAvAnnenSaksbehandler(reservasjon: Reservasjon): String
    fun hentNavnHvisFlyttetAvSaksbehandler(flyttetAv: String): String
}
