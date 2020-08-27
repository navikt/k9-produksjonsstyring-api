package no.nav.k9.integrasjon.abac

import io.ktor.util.KtorExperimentalAPI

interface IPepClient {
    @KtorExperimentalAPI
    suspend fun erOppgaveStyrer(): Boolean

    @KtorExperimentalAPI
    suspend fun harBasisTilgang(): Boolean

    @KtorExperimentalAPI
    suspend fun kanLeggeUtDriftsmelding(): Boolean

    @KtorExperimentalAPI
    suspend fun harTilgangTilLesSak(
        fagsakNummer: String,
        aktørid: String
    ): Boolean

    @KtorExperimentalAPI
    suspend fun harTilgangTilReservingAvOppgaver(): Boolean

    @KtorExperimentalAPI
    suspend fun kanSendeSakTilStatistikk(
        fagsakNummer: String
    ): Boolean
}
