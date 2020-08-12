package no.nav.k9.tjenester.admin

import no.nav.k9.domene.repository.DriftsmeldingRepository
import no.nav.k9.tjenester.driftsmeldinger.Driftsmelding

class AdminTjeneste(
    private val driftsmeldingRepository: DriftsmeldingRepository
) {
    fun setDriftsmelding(driftsmelding: Driftsmelding) {
        driftsmeldingRepository.lagreDriftsmelding(driftsmelding = driftsmelding)
    }
    fun hentDriftsmeldinger(): List<Driftsmelding> {
        return driftsmeldingRepository.hentAlle()
    }
}