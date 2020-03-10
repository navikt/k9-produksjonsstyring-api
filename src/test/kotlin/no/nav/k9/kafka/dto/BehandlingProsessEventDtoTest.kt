package no.nav.k9.kafka.dto

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.dusseldorf.ktor.jackson.dusseldorfConfigured
import org.junit.Test


internal class BehandlingProsessEventDtoTest {
    @Test
    internal fun `Skal kunne serialisere event`() {
        val json =
            """{
  "eksternId": "e84300c6-8976-46fa-8a68-9c7ac27ee636",
  "fagsystem": "FPSAK",
  "saksnummer": "5YC7C",
  "aktørId": "9916108039470",
  "behandlingId": 1000001,
  "eventTid": null,
  "eventHendelse": "BEHANDLINGSKONTROLL_EVENT",
  "behandlinStatus": "UTRED",
  "behandlingStatus": null,
  "behandlingSteg": "INREG",
  "behandlendeEnhet": "0300",
  "ytelseTypeKode": "PSB",
  "behandlingTypeKode": "BT-002",
  "opprettetBehandling": "2020-02-19T08:31:56",
  "aksjonspunktKoderMedStatusListe": {}
}"""
        val objectMapper = jacksonObjectMapper()
            .dusseldorfConfigured().setPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CAMEL_CASE)
        objectMapper.readValue(json, BehandlingProsessEventDto::class.java)
    }
}