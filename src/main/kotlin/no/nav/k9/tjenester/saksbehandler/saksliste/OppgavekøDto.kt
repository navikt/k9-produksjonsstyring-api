package no.nav.k9.tjenester.saksbehandler.saksliste

import no.nav.k9.domene.modell.BehandlingType
import no.nav.k9.domene.modell.FagsakYtelseType
import no.nav.k9.domene.modell.Saksbehandler
import java.time.LocalDate
import java.util.*

class OppgavekøDto(
    val id: UUID,
    val navn: String,
    var sortering: SorteringDto,
    val behandlingTyper: List<BehandlingType>,
    val fagsakYtelseTyper: List<FagsakYtelseType>,
    val sistEndret: LocalDate,
    var antallBehandlinger: Int,
    val tilBeslutter: Boolean,
    val utbetalingTilBruker: Boolean,
    val selvstendigFrilans: Boolean,
    val kombinert: Boolean,
    val søktGradering: Boolean,
    val registrerPapir: Boolean,
    val saksbehandlere: List<Saksbehandler>
)
