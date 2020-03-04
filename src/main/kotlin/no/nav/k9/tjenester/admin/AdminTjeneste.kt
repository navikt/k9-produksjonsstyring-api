package no.nav.k9.tjenester.admin

import no.nav.k9.domene.lager.oppgave.Oppgave
import no.nav.k9.domene.lager.oppgave.OppgaveEventLogg
import no.nav.k9.domene.lager.oppgave.TilbakekrevingOppgave
import java.util.*

interface AdminTjeneste {
    fun synkroniserOppgave(behandlingId: Long?): Oppgave?
    fun hentOppgave(behandlingId: Long?): Oppgave?
    fun hentTilbakekrevingOppgave(uuid: UUID?): TilbakekrevingOppgave?
    fun hentEventer(verdi: Long?): List<OppgaveEventLogg?>?
    fun oppdaterOppgave(behandlingId: Long?)
    fun prosesserAlleMeldingerFraFeillogg(): Int
    fun hentAlleOppgaverForBehandling(behandlingId: Long?): List<Oppgave?>?
    fun deaktiverOppgave(oppgaveId: Long?): Oppgave?
    fun aktiverOppgave(oppgaveId: Long?): Oppgave?
}