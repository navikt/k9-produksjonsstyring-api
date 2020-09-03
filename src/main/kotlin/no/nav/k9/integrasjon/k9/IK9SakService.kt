package no.nav.k9.integrasjon.k9

import no.nav.k9.sak.kontrakt.behandling.BehandlingIdListe

interface IK9SakService {
    suspend fun refreshBehandlinger(behandlingIdList: BehandlingIdListe)
}