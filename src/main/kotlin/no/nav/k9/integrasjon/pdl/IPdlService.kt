package no.nav.k9.integrasjon.pdl

import io.ktor.util.KtorExperimentalAPI

interface IPdlService {
    @KtorExperimentalAPI
    suspend fun person(aktorId: String): PersonPdlResponse

    @KtorExperimentalAPI
    suspend fun identifikator(fnummer: String): PdlResponse
}
