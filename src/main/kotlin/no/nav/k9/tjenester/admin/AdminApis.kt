package no.nav.k9.tjenester.admin

import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.routing.Route
import kotlinx.coroutines.launch
import no.nav.k9.domene.repository.BehandlingProsessEventRepository
import no.nav.k9.domene.repository.OppgaveKøRepository
import no.nav.k9.domene.repository.OppgaveRepository
import no.nav.k9.domene.repository.ReservasjonRepository

@KtorExperimentalLocationsAPI
fun Route.AdminApis(
    behandlingProsessEventRepository: BehandlingProsessEventRepository,
    oppgaveRepository: OppgaveRepository,
    reservasjonRepository: ReservasjonRepository,
    oppgaveKøRepository: OppgaveKøRepository
) {
    @Location("/admin/synkroniseroppgave")
    class synkroniserOppgave

    get { _: synkroniserOppgave ->
        launch {
            val hentAktiveOppgaver = oppgaveRepository.hentAktiveOppgaver()

            for (aktivOppgave in hentAktiveOppgaver) {
                val event = behandlingProsessEventRepository.hent(aktivOppgave.eksternId)
                val oppgave = event.oppgave()
                oppgaveRepository.lagre(oppgave.eksternId) {
                    oppgave
                }

                for (oppgavekø in oppgaveKøRepository.hent()) {
                    oppgaveKøRepository.lagre(oppgavekø.id) { forrige ->
                        forrige?.leggOppgaveTilEllerFjernFraKø(oppgave, reservasjonRepository)
                        forrige!!
                    }
                }
            }
        }
    }

    @Location("/admin/sepaaoppgave")
    class hentOppgave

    get { _: hentOppgave ->
    }

    @Location("/admin/sepaaeventer")
    class hentEventlogg

    get { _: hentEventlogg ->
    }

    @Location("/admin/oppdateringavoppgave")
    class oppdaterOppgave

    get { _: oppdaterOppgave ->
    }

    @Location("/admin/prosesser-melding")
    class prosesserMelding

    get { _: prosesserMelding ->
    }

    @Location("/admin/hent-alle-oppgaver-knyttet-til-behandling")
    class hentAlleOppgaverForBehandling

    get { _: hentAlleOppgaverForBehandling ->
    }

    @Location("/admin/deaktiver-oppgave")
    class deaktiverOppgave

    get { _: deaktiverOppgave ->
    }

    @Location("/admin/aktiver-oppgave")
    class aktiverOppgave

    get { _: aktiverOppgave ->
    }
}