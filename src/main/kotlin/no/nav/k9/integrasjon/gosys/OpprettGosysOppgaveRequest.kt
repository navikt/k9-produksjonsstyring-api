package no.nav.k9.integrasjon.gosys

import no.nav.k9.domene.typer.AktørId
import org.apache.http.message.BasicHeader
import org.json.JSONObject
import java.time.LocalDate

class OpprettGosysOppgaveRequest(
    private val tokenType: String,
    private val accessToken: String,
    private val correlationId: String,
    private val oppgaveType: GosysKonstanter.OppgaveType,
    private val prioritet: GosysKonstanter.Prioritet,
    private val temaGruppe: GosysKonstanter.TemaGruppe,
    private val journalpostSystem: GosysKonstanter.JournalpostSystem,
    private val fagsaksystem: GosysKonstanter.Fagsaksystem,
    private val fristIDager: Int,
    private val aktiv: LocalDate,
    private val aktørId: String,
    private val enhetsNummer: String,
    private val fagsakId: String,
    private val journalpostId: String
) {

    fun headers(): Array<BasicHeader> {
        return mapOf(
            "Authorization" to "$tokenType $accessToken",
            "X-Correlation-ID" to correlationId,
            "Accept" to "application/json"
        ).map { entry -> BasicHeader(entry.key, entry.value) }.toTypedArray()
    }

    fun body(): String {
        return JSONObject()
            .put("oppgavetype", oppgaveType.dto)
            .put("prioritet", prioritet.dto)
            .put("behandlingstema", "kategori.behandlingstema.dto")
            .put("temagruppe", temaGruppe.dto)
            .put("fristFerdigstillelse", Virkedager.nVirkedagerFra(fristIDager, aktiv))
            .put("aktivDato", aktiv.toString())
            .put("journalpostkilde", journalpostSystem.dto)
            .put("aktoerId", aktørId)
            .put("tildeltEnhetsnr", enhetsNummer)
            .put("saksreferanse", fagsakId)
            .put("behandlingstype", "kategori.behandlingstype.dto")
            .put("tema", "kategori.tema.dto")
            .put("behandlesAvApplikasjon", fagsaksystem.dto)
            .put("journalpostId", journalpostId)
            .toString()
    }
}