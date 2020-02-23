package no.nav.k9.integrasjon.gosys

import no.nav.k9.aksjonspunktbehandling.objectMapper
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.util.*

class GosysOppgaveGateway(val httpClient: CloseableHttpClient, val uri: URI) {
    fun opprettOppgave(request: OpprettGosysOppgaveRequest): GosysOppgave {
        val httpPost = HttpPost(uri)
        val entity = StringEntity(request.body())
        httpPost.entity = entity
        httpPost.setHeaders(request.headers())

        val responseEntity = httpClient.execute(httpPost, ForventetStatusCodeResponseHandler(201))
        return objectMapper().readValue(responseEntity, GosysOppgave::class.java)
    }

    fun hentOppgaver(request: HentGosysOppgaverRequest): MutableList<GosysOppgave> {
        val httpGet = HttpGet(uri.toString() + request.queryString())
        httpGet.setHeaders(request.headers())

        val responseEntity = httpClient.execute(httpGet, ForventetStatusCodeResponseHandler(201))
        val json = JSONObject(responseEntity)
        val oppgaver: JSONArray = json.getJSONArray("oppgaver")

        val gosysOppgaver: MutableList<GosysOppgave> = ArrayList()
        oppgaver.forEach { oppgave ->
            val id = (oppgave as JSONObject).getLong("id")
            gosysOppgaver.add(
                GosysOppgave(id)
            )
        }

        return gosysOppgaver
    }
}