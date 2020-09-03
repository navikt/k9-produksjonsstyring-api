package no.nav.k9.integrasjon.k9

import io.ktor.util.*
import no.nav.k9.sak.kontrakt.behandling.BehandlingIdListe

open class K9SakServiceLocal @KtorExperimentalAPI constructor(
) : IK9SakService {
    override suspend fun refreshBehandlinger(behandlingIdList: BehandlingIdListe) {

    }
}