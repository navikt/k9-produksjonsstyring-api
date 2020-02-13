package no.nav.k9.tjenester.avdelingsleder

data class InnloggetNavAnsatt(
    val brukernavn: String,
    val navn: String,
    val kanSaksbehandle: Boolean,
    val kanVeilede: Boolean,
    val kanBeslutte: Boolean,
    val kanBehandleKodeEgenAnsatt: Boolean,
    val kanBehandleKode6: Boolean,
    val kanBehandleKode7: Boolean,
    val kanOppgavestyre: Boolean
)
