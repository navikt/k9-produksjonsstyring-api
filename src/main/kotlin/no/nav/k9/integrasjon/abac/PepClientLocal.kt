package no.nav.k9.integrasjon.abac

import io.ktor.util.*

class PepClientLocal @KtorExperimentalAPI constructor() : IPepClient {
    @KtorExperimentalAPI
    override suspend fun erOppgaveStyrer(): Boolean {
        return true
    }

    @KtorExperimentalAPI
    override suspend fun harBasisTilgang(): Boolean {
        return true
    }

    @KtorExperimentalAPI
    override suspend fun harTilgangTilLesSak(fagsakNummer: String, aktørid: String): Boolean {
        return true
    }

    @KtorExperimentalAPI
    override suspend fun harTilgangTilReservingAvOppgaver(): Boolean {
        return true
    }

    @KtorExperimentalAPI
    override suspend fun harTilgangTilSkjermet(): Boolean {
        return false
    }

    @KtorExperimentalAPI
    override suspend fun kanSendeSakTilStatistikk(fagsakNummer: String): Boolean {
        return true
    }

    @KtorExperimentalAPI
    override suspend fun erSakKode6(fagsakNummer: String): Boolean {
        return false
    }

}