package no.nav.k9.domene.modell

import no.nav.k9.domene.modell.punsj.akjonspunkter.Aksjonspunkt
import no.nav.k9.kodeverk.api.Kodeverdi
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon

class AksjonspunktDefWrapper {

    companion object {
        fun fraKode(kode: String) {
            AksjonspunktDefinisjon.fraKode(kode)
        }
    }


    enum class AksjonspunktDefinisjonPunsj(override val kode: String, override val navn: String) : Kodeverdi {

        PUNSJ("PUNSJ", "Punsj oppgave"),
        Aksjonspunkt.PUNSJ_HAR_UTLØPT("UTLØPT", "Utløpt oppgave"),
        VENTER_PÅ_INFORMASJON("MER_INFORMASJON", "Venter på informasjon")
    }


}
