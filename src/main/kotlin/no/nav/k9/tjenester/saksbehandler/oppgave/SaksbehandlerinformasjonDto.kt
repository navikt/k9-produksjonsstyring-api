package no.nav.k9.tjenester.saksbehandler.oppgave

class SaksbehandlerinformasjonDto(
    val saksbehandlerIdent: String,
    val navn: String,
    val avdelinger: List<String>
) {

    override fun toString(): String {
        return "SaksbehandlerinformasjonDto{" +
                ", saksbehandlerIdent='" + saksbehandlerIdent + '\'' +
                ", navn='" + navn + '\'' +
                ", avdelinger=" + avdelinger +
                '}'
    }

}