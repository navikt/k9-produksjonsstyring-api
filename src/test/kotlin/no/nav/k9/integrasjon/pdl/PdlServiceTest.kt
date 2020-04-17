package no.nav.k9.integrasjon.pdl

import no.nav.k9.domene.oppslag.Ident
import org.junit.Test
import kotlin.test.assertEquals

class PdlServiceTest {
    @Test
    fun `Skal bytte ut identer`() {

        val likIdent = PdlService.getQ2Ident(Ident("14128521632"))
        assertEquals("14128521632", likIdent)

        val likIdent2 = PdlService.getQ2Ident(Ident("19128521618"))
        assertEquals("19128521618", likIdent2)

        val ulikIdent = PdlService.getQ2Ident(Ident("39234523456"))
        assertEquals("14088521472", ulikIdent)
        
    }
}