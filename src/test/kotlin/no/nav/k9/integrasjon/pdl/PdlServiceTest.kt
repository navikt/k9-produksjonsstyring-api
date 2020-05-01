package no.nav.k9.integrasjon.pdl

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.k9.aksjonspunktbehandling.objectMapper
import org.junit.Test
import kotlin.test.assertEquals

class PdlServiceTest {
    @Test
    fun `Skal bytte ut identer`() {

        val likIdent = PdlService.getQ2Ident("14128521632")
        assertEquals("14128521632", likIdent)

        val likIdent2 = PdlService.getQ2Ident("19128521618")
        assertEquals("19128521618", likIdent2)

        val ulikIdent = PdlService.getQ2Ident("39234523456")
        assertEquals("14088521472", ulikIdent)
    }

    @Test
    fun `skal desrialisere personpdl`() {        
        val json = "{\n  \"data\": {\n    \"hentPerson\": {\n      \"navn\": [\n        {\n          \"fornavn\": \"GRØNN\",\n          \"mellomnavn\": null,\n          \"etternavn\": \"STAFFELI\",\n          \"forkortetNavn\": \"STAFFELI GRØNN\"\n        }\n      ],\n      \"folkeregisteridentifikator\": [\n        {\n          \"identifikasjonsnummer\": \"19128521618\"\n        }\n      ],\n      \"kjoenn\": [\n        {\n          \"kjoenn\": \"KVINNE\"\n        }\n      ],\n      \"doedsfall\": []\n    }\n  }\n}"
        val readValue = objectMapper().readValue<PersonPdl>(json)
        assertEquals("KVINNE", readValue.data.hentPerson.kjoenn[0].kjoenn)
    }
}