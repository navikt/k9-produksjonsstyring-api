package no.nav.k9.integrasjon.omsorgspenger

interface IOmsorgspengerService {

    suspend fun hentOmsorgspengerSakDto(identitetsnummer: String): OmsorgspengerSakDto?

}
