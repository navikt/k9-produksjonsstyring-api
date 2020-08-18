package no.nav.k9.integrasjon.azuregraph

import io.ktor.util.KtorExperimentalAPI

interface IAzureGraphService {
    @KtorExperimentalAPI
    suspend fun hentIdentTilInnloggetBruker(): String

    @KtorExperimentalAPI
    suspend fun hentEnhetForInnloggetBruker(): String
}