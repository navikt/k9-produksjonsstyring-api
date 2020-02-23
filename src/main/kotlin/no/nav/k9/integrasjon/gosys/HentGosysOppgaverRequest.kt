package no.nav.k9.integrasjon.gosys

import org.apache.http.message.BasicHeader

data class HentGosysOppgaverRequest(
    val tokenType: String,
    val accessToken: String,
    val correlationId: String,
    val tema: GosysKonstanter.Tema,
    val oppgaveType: GosysKonstanter.OppgaveType,
    val journalpostId: String,
    val aktørId: String
){
    fun queryString(): String {
        return "?tema=${tema.dto}&oppgavetype=${oppgaveType.dto}&journalpostId=$journalpostId&aktoerId=$aktørId"
    }
    fun headers(): Array<BasicHeader> {
      return  mapOf(
            "Authorization" to "$tokenType $accessToken",
            "X-Correlation-ID" to correlationId,
            "Accept" to "application/json"
        ).map { entry -> BasicHeader(entry.key,entry.value)  }.toTypedArray()
    }
}