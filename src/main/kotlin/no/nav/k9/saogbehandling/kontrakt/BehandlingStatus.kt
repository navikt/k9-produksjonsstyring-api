package no.nav.k9.saogbehandling.kontrakt

import no.nav.melding.virksomhet.behandlingsstatus.hendelsehandterer.v1.hendelseshandtererbehandlingsstatus.Hendelse


open class BehandlingStatus : Hendelse() {
    private val behandlingsID: String? = null
    private val behandlingstype: Behandlingstyper? = null
    private val sakstema: Sakstemaer? = null
    private val behandlingstema: Behandlingstemaer? = null
    private val aktoerREF: List<Aktoer>? = null
    private val ansvarligEnhetREF: String? = null
    private val primaerBehandlingREF: PrimaerBehandling? = null
    private val sekundaerBehandlingREF: List<SekundaerBehandling>? = null
    private val applikasjonSakREF: String? = null
    private val applikasjonBehandlingREF: String? = null
    private val styringsinformasjonListe: List<Parameter>? = null
}