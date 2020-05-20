package no.nav.k9.tjenester.avdelingsleder.oppgaveko

import no.nav.k9.domene.modell.AndreKriterierType

data class AndreKriterierDto(
    val id: String,
    val andreKriterierType: AndreKriterierType,
    val checked: Boolean,
    val inkluder: Boolean
)
