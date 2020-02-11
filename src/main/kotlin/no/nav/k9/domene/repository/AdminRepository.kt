//package no.nav.k9.domene.repository
//
//import no.nav.foreldrepenger.loslager.oppgave.EventmottakFeillogg
//import no.nav.foreldrepenger.loslager.oppgave.Oppgave
//import no.nav.foreldrepenger.loslager.oppgave.OppgaveEventLogg
//import no.nav.foreldrepenger.loslager.oppgave.TilbakekrevingOppgave
//import no.nav.k9.domene.lager.oppgave.EventmottakFeillogg
//import no.nav.k9.domene.lager.oppgave.Oppgave
//import no.nav.k9.domene.lager.oppgave.OppgaveEventLogg
//import no.nav.k9.domene.lager.oppgave.TilbakekrevingOppgave
//import java.util.UUID
//
//interface AdminRepository {
//    fun deaktiverSisteOppgave(behandlingId: Long?)
//
//    fun hentSisteOppgave(behandlingId: Long?): Oppgave
//
//    fun hentSisteTilbakekrevingOppgave(behandlingId: UUID): TilbakekrevingOppgave
//
//    fun hentEventer(behandlingId: Long?): List<OppgaveEventLogg>
//
//    fun hentAlleAktiveOppgaver(): List<Oppgave>
//
//    fun hentAlleMeldingerFraFeillogg(): List<EventmottakFeillogg>
//
//    fun markerFerdig(feilloggId: Long?)
//
//    fun hentAlleOppgaverForBehandling(behandlingId: Long?): List<Oppgave>
//
//    fun deaktiverOppgave(oppgaveId: Long?): Oppgave
//
//    fun aktiverOppgave(oppgaveId: Long?): Oppgave
//}
