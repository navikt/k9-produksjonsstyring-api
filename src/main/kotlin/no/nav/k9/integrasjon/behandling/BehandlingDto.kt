package no.nav.k9.integrasjon.behandling

import ResourceLink
import no.nav.k9.domene.lager.oppgave.BehandlingStatus
import no.nav.k9.domene.modell.BehandlingType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class BehandlingDto(
    var id: Long,
    var uuid: UUID,
    var versjon: Long,
    var type: BehandlingType,
    var status: BehandlingStatus,
    var fagsakId: Long,
    var opprettet: LocalDateTime,
    var avsluttet: LocalDateTime,
    var endret: LocalDateTime,
    var endretAvBrukernavn: String,
    var behandlendeEnhetId: String,
    var behandlendeEnhetNavn: String,
    var erAktivPapirsoknad: Boolean = false,
    var behandlingsfristTid: LocalDate
) {
    private val links: List<ResourceLink> = ArrayList<ResourceLink>()

    open fun getLinks(): List<ResourceLink> {
        return Collections.unmodifiableList(links)
    }
}