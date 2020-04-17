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
                val etternavn: String,
                val forkortetNavn: String,
                val fornavn: String,
                val mellomnavn: Any?
            )
        }
    }
}