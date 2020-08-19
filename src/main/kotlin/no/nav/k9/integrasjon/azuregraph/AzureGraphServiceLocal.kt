package no.nav.k9.integrasjon.azuregraph

import io.ktor.util.KtorExperimentalAPI

open class AzureGraphServiceLocal @KtorExperimentalAPI constructor() : IAzureGraphService {
    @KtorExperimentalAPI
    override suspend fun hentIdentTilInnloggetBruker(): String {
        return "saksbehandler@nav.no"
    }

    @KtorExperimentalAPI
    override suspend fun hentEnhetForInnloggetBruker(): String {
        return "saksbehandler@nav.no"
    }

}