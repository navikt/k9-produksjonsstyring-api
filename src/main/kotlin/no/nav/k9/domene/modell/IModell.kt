package no.nav.k9.domene.modell

import no.nav.k9.domene.lager.oppgave.Oppgave
import no.nav.k9.domene.repository.ReservasjonRepository
import no.nav.k9.domene.repository.SaksbehandlerRepository
import no.nav.k9.integrasjon.kafka.dto.BehandlingProsessEventDto
import no.nav.k9.statistikk.kontrakter.Behandling
import no.nav.k9.statistikk.kontrakter.Sak

interface IModell {
    fun oppgave(sisteEvent: BehandlingProsessEventDto = sisteEvent()): Oppgave
    fun sisteEvent(): BehandlingProsessEventDto
    fun forrigeEvent(): BehandlingProsessEventDto?
    fun førsteEvent(): BehandlingProsessEventDto
    fun starterSak(): Boolean
    fun erTom(): Boolean
    fun dvhSak(): Sak
    fun dvhBehandling(
        saksbehandlerRepository: SaksbehandlerRepository,
        reservasjonRepository: ReservasjonRepository
   ): Behandling

    // Array med alle versjoner av modell basert på eventene, brukes når man skal spille av eventer
   fun alleVersjoner(): MutableList<K9SakModell>
}