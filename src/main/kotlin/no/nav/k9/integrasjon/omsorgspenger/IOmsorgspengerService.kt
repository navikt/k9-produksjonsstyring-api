package no.nav.k9.integrasjon.omsorgspenger

import io.ktor.util.*

interface IOmsorgspengerService {

    @KtorExperimentalAPI
    suspend fun hentOmsorgspengerSakDto(sakFnrDto: OmsorgspengerSakFnrDto): OmsorgspengerSakDto?

}
