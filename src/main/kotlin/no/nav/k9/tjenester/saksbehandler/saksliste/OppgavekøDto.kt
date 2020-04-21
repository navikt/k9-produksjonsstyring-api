package no.nav.k9.tjenester.saksbehandler.saksliste

import no.nav.k9.domene.modell.AndreKriterierType
import no.nav.k9.domene.modell.*
import java.time.LocalDate
import java.util.*

class OppgavekøDto(
    val id: UUID,
    var navn: String,
    var sortering: SorteringDto,
    var behandlingTyper: List<BehandlingType>,
    var fagsakYtelseTyper: List<FagsakYtelseType>,
    var andreKriterier: List<AndreKriterierType>,
    val sistEndret: LocalDate,
    var antallBehandlinger: Int,
    val saksbehandlere: List<Saksbehandler>
)
