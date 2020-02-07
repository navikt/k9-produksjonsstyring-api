package no.nav.helse

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.helse.dusseldorf.ktor.core.fromResources
import java.util.*

private const val dokarkivBasePath = "/dokarkiv-mock"
private const val dokarkivMottaInngaaendeForsendelsePath = "$dokarkivBasePath/rest/journalpostapi/v1/journalpost"
private const val pleiepengerDokumentPath = "/pleiepenger-dokument-mock"

internal fun stubMottaInngaaendeForsendelseOk(
    tilstand: String) {
    WireMock.stubFor(
        WireMock.post(WireMock.urlMatching(".*$dokarkivMottaInngaaendeForsendelsePath"))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                            {
                              "journalpostId": "466985833",
                              "journalstatus": "M",
                              "melding": null,
                              "journalpostferdigstilt": false,
                              "dokumenter": [
                                {
                                  "dokumentInfoId": "485201432"
                                },
                                {
                                  "dokumentInfoId": "485201433"
                                }
                              ]
                            }
                        """.trimIndent())
            )
    )
}

internal fun WireMockServer.stubDomotInngaaendeIsReady() : WireMockServer {
    WireMock.stubFor(
        WireMock.get(WireMock.urlMatching(".*$dokarkivBasePath/isReady"))
            .willReturn(
                WireMock.aResponse().withStatus(200)
            )
    )
    return this
}

internal fun WireMockServer.stubGetDokument() : WireMockServer {
    val content = Base64.getEncoder().encodeToString("iPhone_6.jpg".fromResources().readBytes())
    WireMock.stubFor(
        WireMock.get(WireMock.urlPathMatching(".*$pleiepengerDokumentPath.*"))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                            {
                                "content": "$content",
                                "content_type": "image/jpeg",
                                "title": "Dette er en tittel"
                            }
                        """.trimIndent()
                    )
            )
    )
    return this
}

internal fun stubGetDokumentJson(
    dokumentId: String
) {
    val content = Base64.getEncoder().encodeToString("jwkset.json".fromResources().readBytes())
    WireMock.stubFor(
        WireMock.get(WireMock.urlPathMatching(".*$pleiepengerDokumentPath.*/$dokumentId"))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                            {
                                "content": "$content",
                                "content_type": "application/json",
                                "title": "Dette er en tittel"
                            }
                        """.trimIndent()
                    )
            )
    )
}

internal fun stubGetDokumentPdf(
    dokumentId: String
) {
    val content = Base64.getEncoder().encodeToString("test.pdf".fromResources().readBytes())
    WireMock.stubFor(
        WireMock.get(WireMock.urlPathMatching(".*$pleiepengerDokumentPath.*/$dokumentId"))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                            {
                                "content": "$content",
                                "content_type": "application/pdf",
                                "title": "Dette er en tittel"
                            }
                        """.trimIndent()
                    )
            )
    )
}


internal fun WireMockServer.getDokarkivUrl() = baseUrl() + dokarkivBasePath
internal fun WireMockServer.getPleiepengerDokumentUrl() = baseUrl() + pleiepengerDokumentPath