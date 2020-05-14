package no.nav.k9.tjenester.mock

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon

class Aksjonspunkter {
    fun aksjonspunkter(): List<Aksjonspunkt> {

        return AksjonspunktDefinisjon.values().map { Aksjonspunkt(
            kode = it.kode,
            navn = it.navn,
            aksjonspunktype = it.aksjonspunktType.name,
            behandlingsstegtype = it.behandlingSteg.name,
            plassering = "",
            totrinn = it.defaultTotrinnBehandling,
            vilkårtype = it.vilkårType.name
        ) }
    }
}

data class Aksjonspunkt(
    val kode: String,
    val navn: String,
    val aksjonspunktype: String,
    val behandlingsstegtype: String,
    val plassering: String,
    val vilkårtype: String?,
    val totrinn: Boolean,
    var antall: Int = 0 
)