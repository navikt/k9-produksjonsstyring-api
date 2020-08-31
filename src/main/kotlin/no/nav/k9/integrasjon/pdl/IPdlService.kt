package no.nav.k9.integrasjon.pdl

import io.ktor.util.KtorExperimentalAPI

interface IPdlService {
    @KtorExperimentalAPI
    suspend fun person(aktorId: String): PersonPdl?

    @KtorExperimentalAPI
    suspend fun identifikator(fnummer: String): PdlResponse
}
