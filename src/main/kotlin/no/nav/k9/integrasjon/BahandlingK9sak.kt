package no.nav.k9.integrasjon

import java.time.LocalDate
import java.util.*

data class BehandlingK9sak(
    val behandlingId: Long,
    val uuid: UUID,
    val status: String,
    val behandlingstidFrist: LocalDate,
    val type: String,
    val tema: String,
    val Årsak: String,
    val behandlendeEnhet: String,
    val behandlendeEnhetNavn: String,
    val ansvalligSaksbehandler: String,
    val førsteUttaksdag: LocalDate,
    val inntektsmeldinger: List<String>,
    val aksjonspunkter: List<Aksjonspunkt>,
    var erUtlandssak: Boolean,
    val harRefusjonskravFraArbeidsgiver: Boolean,
    var harGradering: Boolean,
    var harVurderSykdom: Boolean
)