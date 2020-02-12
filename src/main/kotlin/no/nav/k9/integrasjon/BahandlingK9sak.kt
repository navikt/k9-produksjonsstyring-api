package no.nav.k9.integrasjon

import java.time.LocalDate
import java.util.*


data class BehandlingK9sak(
    var behandlingId: Long,
    var uuid: UUID,
    var status: String,
    var behandlingstidFrist: LocalDate,
    var type: String,
    var tema: String,
    var Årsak: String,
    var behandlendeEnhet: String,
    var behandlendeEnhetNavn: String,
    var ansvarligSaksbehandler: String,
    var førsteUttaksdag: LocalDate,
    var inntektsmeldinger: List<String>,
    var aksjonspunkter: List<Aksjonspunkt>,
    var erUtlandssak: Boolean,
    var harRefusjonskravFraArbeidsgiver: Boolean,
    var harGradering: Boolean,
    var harVurderSykdom: Boolean
)