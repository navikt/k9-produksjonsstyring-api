package no.nav.k9.integrasjon.pdl

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer
import java.time.LocalDate


data class PersonPdl(
    val `data`: Data
) {
    data class Data(
        val hentPerson: HentPerson
    ) {
        data class HentPerson(
            val folkeregisteridentifikator: List<Folkeregisteridentifikator>,
            val navn: List<Navn>,
            val kjoenn: Kjoenn,
            val doedsfall: Doedsfall?
        ) {
            data class Kjoenn(
                val kjoenn: String
            )

            data class Doedsfall(
                @JsonSerialize(using = ToStringSerializer::class)
                @JsonDeserialize(using = LocalDateDeserializer::class)
                val doedsdato: LocalDate
            )

            data class Folkeregisteridentifikator(
                val identifikasjonsnummer: String
            )

            data class Navn(
                val etternavn: String,
                val forkortetNavn: String,
                val fornavn: String,
                val mellomnavn: String?
            )
        }
    }
}
