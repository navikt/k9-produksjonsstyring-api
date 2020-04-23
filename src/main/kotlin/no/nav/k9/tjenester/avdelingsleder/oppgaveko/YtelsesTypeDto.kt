package no.nav.k9.tjenester.avdelingsleder.oppgaveko

import no.nav.k9.domene.modell.FagsakYtelseType

data class YtelsesTypeDto(
    val id: String,
    val fagsakYtelseType: FagsakYtelseType,
    val checked: Boolean
)
