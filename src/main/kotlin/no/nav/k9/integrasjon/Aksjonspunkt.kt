package no.nav.k9.integrasjon

import java.time.LocalDateTime

data class Aksjonspunkt(
    val definisjonKode: String,
    val statusKode: String,
    val begrunnelse: String,
    val fristTid: LocalDateTime
) {
    private val PÅ_VENT_KODEGRUPPE_STARTS_WITH = "7"
    private val MANUELT_SATT_PÅ_VENT_KODE = "7001";
    private val STATUSKODE_AKTIV = "OPPR"
    private val TIL_BESLUTTER_KODE = "5016";
    private val REGISTRER_PAPIRSØKNAD_KODE =
        listOf("5012", "5040", "5057", "5096")

    fun erPåVent(): Boolean {
        return definisjonKode.startsWith(PÅ_VENT_KODEGRUPPE_STARTS_WITH) && erAktiv()
    }

    fun erManueltPåVent(): Boolean {
        return MANUELT_SATT_PÅ_VENT_KODE == definisjonKode && erAktiv()
    }

    fun erAktiv(): Boolean {
        return STATUSKODE_AKTIV == statusKode
    }

    fun tilBeslutter(): Boolean {
        return TIL_BESLUTTER_KODE == definisjonKode && erAktiv()
    }

    fun erRegistrerPapirSøknad(): Boolean {
        return REGISTRER_PAPIRSØKNAD_KODE.contains(definisjonKode) && erAktiv()
    }
}

