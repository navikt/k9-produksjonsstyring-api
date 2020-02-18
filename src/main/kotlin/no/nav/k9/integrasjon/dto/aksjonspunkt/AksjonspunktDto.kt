package no.nav.k9.integrasjon.dto.aksjonspunkt

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.k9.integrasjon.dto.kodeverk.KodeDto
import java.time.LocalDateTime

@JsonIgnoreProperties(ignoreUnknown = true)
class AksjonspunktDto(
    var definisjon: KodeDto,
    var status: KodeDto,
    var begrunnelse: String,
    var fristTid: LocalDateTime
) {

    override fun toString(): String {
        return "AksjonspunktDto{" +
                "definisjon=" + definisjon +
                ", status=" + status +
                ", begrunnelse='" + begrunnelse + '\'' +
                ", fristTid=" + fristTid +
                '}'
    }

}