package no.nav.k9.integrasjon.pdl

import java.time.LocalDate


data class PersonPdl(
    val `data`: Data
) {
    data class Data(
        val hentPerson: HentPerson
    ) {
        data class HentPerson(
            val folkeregisteridentifikator: List<Folkeregisteridentifikator>,
            val navn: List<Navn>
        ) {
            data class Kjoenn(
                val kjoenn: String
            )
            
            data class Doedsfall(
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