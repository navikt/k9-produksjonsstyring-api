package no.nav.k9.tilgangskontroll.abac

interface AbacAttributeId {
    fun getId(): String
}

enum class AbacAttributter(val attributtId: String): AbacAttributeId {
    K9_SAK_ANSVARLIG_SAKSBEHANDLER("no.nav.abac.attributter.resource.k9.sak.ansvarlig_saksbehandler");

    override fun getId(): String = this.attributtId
}