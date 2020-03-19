package no.nav.k9.tjenester.saksbehandler.saksliste

class SaksbehandlerDto(
    val brukerIdent: String,
    val navn: String,
    val avdelingsnavn: List<String>)