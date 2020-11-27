package no.nav.k9.integrasjon.omsorgspenger

import io.ktor.util.*

class OmsorgspengerServiceLocal : IOmsorgspengerService {

    @KtorExperimentalAPI
    override suspend fun hentOmsorgspengerSakDto(sakFnrDto: OmsorgspengerSakFnrDto): OmsorgspengerSakDto? {
        return null
    }
}
