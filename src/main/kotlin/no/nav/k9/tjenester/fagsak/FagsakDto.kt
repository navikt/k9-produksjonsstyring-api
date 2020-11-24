package no.nav.k9.tjenester.fagsak


import no.nav.k9.domene.modell.BehandlingStatus
import no.nav.k9.domene.modell.FagsakStatus
import no.nav.k9.domene.modell.FagsakYtelseType
import no.nav.k9.integrasjon.kafka.dto.Fagsystem
import java.time.LocalDateTime

data class FagsakDto (
    val fagsystem: Fagsystem,
    val saksnummer: String,
    val person: PersonDto,
    val sakstype: FagsakYtelseType,
    val behandlingStatus: BehandlingStatus?,
    val opprettet: LocalDateTime,
    val aktiv: Boolean
)
