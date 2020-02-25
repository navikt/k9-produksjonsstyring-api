package no.nav.k9.integrasjon.gosys

import no.nav.k9.domene.typer.AktørId
import org.apache.http.message.BasicHeader
import org.json.JSONObject
import java.time.LocalDate

class AvsluttGosysOppgaveRequest(
    private val versjon: Int,
    private val id: Int
) {


    fun body(): String {
        return JSONObject()
            .put("versjon", versjon)
            .put("id", id)
            .put("status", GosysKonstanter.OppgaveStatus.FERDIGSTILT)
            .toString()
    }
}