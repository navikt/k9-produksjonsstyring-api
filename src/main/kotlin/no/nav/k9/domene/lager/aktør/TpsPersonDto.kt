package no.nav.k9.domene.lager.aktør

import no.nav.k9.domene.typer.AktørId
import no.nav.k9.domene.typer.PersonIdent
import java.time.LocalDate

data class TpsPersonDto(
    var aktørId: AktørId,
    var navn: String,
    var fnr: PersonIdent,
    var fødselsdato: LocalDate,
    var kjønn: String,
    var dødsdato: LocalDate?,
    var diskresjonskode: String
)
