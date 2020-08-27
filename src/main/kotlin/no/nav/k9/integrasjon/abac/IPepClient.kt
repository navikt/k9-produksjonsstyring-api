package no.nav.k9.integrasjon.abac

import io.ktor.util.*

interface IPepClient {
    @KtorExperimentalAPI
    suspend fun erOppgaveStyrer(): Boolean
    
    @KtorExperimentalAPI
    suspend fun harTilgangTilSkjermet(): Boolean
   
    @KtorExperimentalAPI
    suspend fun harBasisTilgang(): Boolean

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

    @KtorExperimentalAPI
    suspend fun erSakKode6(
        fagsakNummer: String
    ): Boolean
}