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

    fun erPåVent(): Boolean {
        return definisjonKode.startsWith(PÅ_VENT_KODEGRUPPE_STARTS_WITH) && erAktiv()
    }

    fun erManueltPåVent(): Boolean {
        return MANUELT_SATT_PÅ_VENT_KODE == definisjonKode && erAktiv()
    }

    private fun erAktiv(): Boolean {
        return STATUSKODE_AKTIV == statusKode
    }
}

