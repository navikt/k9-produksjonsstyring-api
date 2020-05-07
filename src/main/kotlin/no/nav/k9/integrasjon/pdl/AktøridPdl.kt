package no.nav.k9.integrasjon.pdl


data class AktøridPdl(
    val `data`: Data
) {
    data class Data(
        val hentIdenter: HentIdenter
    ) {
        data class HentIdenter(
            val identer: List<Identer>
        ) {
            data class Identer(
                val gruppe: String,
                val historisk: Boolean,
                var ident: String
            )
        }
    }
}