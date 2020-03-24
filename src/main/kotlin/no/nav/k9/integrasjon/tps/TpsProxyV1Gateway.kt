package no.nav.k9.integrasjon.tps

import no.nav.k9.domene.oppslag.Attributt
import no.nav.k9.domene.oppslag.Ident

class TpsProxyV1Gateway(
    private val tpsProxyV1: TpsProxyV1
) {
    internal companion object {
        private val personAttributter = setOf(
            Attributt.fornavn,
            Attributt.mellomnavn,
            Attributt.etternavn,
            Attributt.fødselsdato
        )

        private val barnAttributter = setOf(
            Attributt.barnAktørId, // Må hente opp barn for å vite hvem vi skal slå opp aktørId på
            Attributt.barnFornavn,
            Attributt.barnMellomnavn,
            Attributt.barnEtternavn,
            Attributt.barnHarSammeAdresse,
            Attributt.barnFødselsdato
        )
    }

    internal suspend fun barn(
        ident: Ident,
        attributter: Set<Attributt>
    ): Set<TpsBarn>? {
        return if (!attributter.any { it in barnAttributter }) null
        else tpsProxyV1.barn(ident)
    }

    internal suspend fun person(
        ident: Ident,
        attributter: Set<Attributt>
    ): TpsPerson? {
        return if (!attributter.any { it in personAttributter }) null
        else tpsProxyV1.person(ident)
    }
}
