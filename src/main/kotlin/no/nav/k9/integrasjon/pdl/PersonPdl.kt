package no.nav.k9.integrasjon.pdl


data class PersonPdl(
    val `data`: Data
) {
    data class Data(
        val hentPerson: HentPerson
    ) {
        data class HentPerson(
            val navn: List<Navn>
        ) {
            data class Navn(
                val etternavn: String, // MIDTPUNKT
                val forkortetNavn: String, // MIDTPUNKT NOBEL
                val fornavn: String, // NOBEL
                val mellomnavn: Any? // null
            )
        }
    }
}