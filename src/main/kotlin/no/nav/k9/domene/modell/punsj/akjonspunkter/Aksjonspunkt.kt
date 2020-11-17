package no.nav.k9.domene.modell.punsj.akjonspunkter

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
enum class Aksjonspunkt(override val kode: String, override val navn: String) : Kodeverdi {
    OPPRETTET("OPPR", "Opprettet"),
    AVSLUTTET("AVSLU", "Avsluttet");

    override val kodeverk = "PUNSJ_OPPGAVE_STATUS"

    companion object {
        @JsonCreator
        @JvmStatic
        fun fraKode(kode: String): Aksjonspunkt = values().find { it.kode == kode }!!
    }
}
