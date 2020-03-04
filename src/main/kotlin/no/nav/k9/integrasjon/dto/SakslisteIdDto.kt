//package no.nav.k9.integrasjon.dto
//
//import com.fasterxml.jackson.annotation.JsonProperty
//import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter
//import no.nav.vedtak.sikkerhet.abac.AbacDto
//import java.util.*
//
//class SakslisteIdDto : AbacDto {
//    @JsonProperty("sakslisteId")
//    val verdi: Long
//
//    constructor(sakslisteId: Long) {
//        Objects.requireNonNull(sakslisteId, "sakslisteId")
//        verdi = sakslisteId
//    }
//
//    constructor(sakslisteId: String?) {
//        verdi = java.lang.Long.valueOf(sakslisteId)
//    }
//
//    override fun toString(): String {
//        return "SaksnummerDto{" +
//                "sakslisteId='" + verdi + '\'' +
//                '}'
//    }
//
//    override fun abacAttributter(): AbacDataAttributter {
//        return AbacDataAttributter.opprett()
//    }
//}