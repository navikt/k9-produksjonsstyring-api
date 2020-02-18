package no.nav.k9.integrasjon

import no.nav.k9.integrasjon.dto.aksjonspunkt.AksjonspunktDto
import java.time.LocalDateTime
import java.util.*

class Aksjonspunkt(
    val definisjonKode: String,
    val statusKode: String,
    val begrunnelse: String,
    val fristTid: LocalDateTime
) {
    private val PÅ_VENT_KODEGRUPPE_STARTS_WITH = "7"
    private val MANUELT_SATT_PÅ_VENT_KODE = "7001";
    private val STATUSKODE_AKTIV = "OPPR"
    val STATUSKODE_AVBRUTT = "AVBR"

    val TIL_BESLUTTER_KODE = "5016"
    val REGISTRER_PAPIRSØKNAD_KODE =
        Arrays.asList("5012", "5040", "5057", "5096")

    val AUTOMATISK_MARKERING_SOM_UTLAND = "5068"
    val MANUELL_MARKERING_SOM_UTLAND = "6068"
    val EØS_BOSATT_NORGE = "EØS_BOSATT_NORGE"
    val BOSATT_UTLAND = "BOSATT_UTLAND"

    fun erPåVent(): Boolean {
        return definisjonKode.startsWith(PÅ_VENT_KODEGRUPPE_STARTS_WITH) && erAktiv()
    }

    fun erManueltPåVent(): Boolean {
        return MANUELT_SATT_PÅ_VENT_KODE == definisjonKode && erAktiv()
    }

    fun erAktiv(): Boolean {
        return STATUSKODE_AKTIV == statusKode
    }

    fun erRegistrerPapirSøknad(): Boolean {
        return REGISTRER_PAPIRSØKNAD_KODE.contains(definisjonKode) && erAktiv()
    }

    fun tilBeslutter(): Boolean {
        return TIL_BESLUTTER_KODE == definisjonKode && erAktiv()
    }

    fun erUtenlandssak(): Boolean {
        return erAutomatiskMarkertSomUtenlandssak() || erManueltMarkertSomUtenlandssak()
    }

    private fun erAutomatiskMarkertSomUtenlandssak(): Boolean {
        return AUTOMATISK_MARKERING_SOM_UTLAND == definisjonKode && !erAvbrutt()
    }

    private fun erManueltMarkertSomUtenlandssak(): Boolean {
        return MANUELL_MARKERING_SOM_UTLAND == definisjonKode && (EØS_BOSATT_NORGE == begrunnelse || BOSATT_UTLAND == begrunnelse)
    }

    fun erAvbrutt(): Boolean {
        return STATUSKODE_AVBRUTT == definisjonKode
    }

    //    public boolean erSelvstendigEllerFrilanser() {
//        return SELVSTENDIG_FRILANSER_GRUPPE.contains(definisjonKode) && erAktiv();
//    }
    fun aksjonspunktFra(aksjonspunktDto: AksjonspunktDto): Aksjonspunkt {
        return Aksjonspunkt(
            aksjonspunktDto.definisjon.kode,
            aksjonspunktDto.status.kode,
            aksjonspunktDto.begrunnelse,
            aksjonspunktDto.fristTid
        )
    }
}

