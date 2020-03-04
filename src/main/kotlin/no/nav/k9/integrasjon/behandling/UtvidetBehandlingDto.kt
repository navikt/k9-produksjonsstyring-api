package no.nav.k9.integrasjon.behandling

import ResourceLink
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.k9.domene.lager.oppgave.BehandlingStatus
import no.nav.k9.domene.modell.BehandlingType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@JsonAutoDetect(
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE,
    fieldVisibility = JsonAutoDetect.Visibility.ANY
)
class UtvidetBehandlingDto(
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
    var erAktivPapirsoknad: Boolean,
    var behandlingsfristTid: LocalDate
) {
    /** Eventuelt async status på tasks.  */


    @JsonProperty("behandlingPaaVent")
    val isBehandlingPåVent = false

    @JsonProperty("behandlingKoet")
    val isBehandlingKoet = false
    @JsonProperty("ansvarligSaksbehandler")
    var ansvarligSaksbehandler = ""
    @JsonProperty("ansvarligBeslutter")
    var ansvarligBeslutter = ""

    @JsonProperty("fristBehandlingPaaVent")
    var fristBehandlingPåVent = ""
    @JsonProperty("venteArsakKode")
    var venteÅrsakKode = ""

    @JsonProperty("behandlingHenlagt")
    val isBehandlingHenlagt = false
    @JsonProperty("toTrinnsBehandling")
    val toTrinnsBehandling = false

    private val links: List<ResourceLink> = ArrayList<ResourceLink>()

    open fun getLinks(): List<ResourceLink> {
        return Collections.unmodifiableList(links)
    }

}