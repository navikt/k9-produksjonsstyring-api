package no.nav.k9.tjenester.avdelingsleder.oppgaveko

import no.nav.k9.domene.modell.FagsakYtelseType

data class YtelsesTypeDto(
    val id: OppgavekøIdDto,
    val ytelseType: FagsakYtelseType,
    val markert: Boolean
)
