package no.nav.k9.tjenester.saksbehandler.saksliste

import no.nav.k9.domene.modell.Enhet

class SaksbehandlerDto(
    val brukerIdent: String?,
    val navn: String?,
    var epost: String,
    val oppgavekoer: List<String>)
