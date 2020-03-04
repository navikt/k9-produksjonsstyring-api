package no.nav.k9.tjenester.saksbehandler.saksliste

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

class SakslisteIdDto  {
    @JsonProperty("sakslisteId")
    val verdi: Long?

    constructor() {
        verdi = null // NOSONAR
    }

    constructor(sakslisteId: Long) {
        Objects.requireNonNull(sakslisteId, "sakslisteId")
        verdi = sakslisteId
    }

    constructor(sakslisteId: String?) {
        verdi = java.lang.Long.valueOf(sakslisteId)
    }

    override fun toString(): String {
        return "SaksnummerDto{" +
                "sakslisteId='" + verdi + '\'' +
                '}'
    }
}