package no.nav.k9.tjenester.driftsmeldinger

import no.nav.k9.domene.repository.DriftsmeldingRepository

class DriftsmeldingTjeneste(
    private val driftsmeldingRepository: DriftsmeldingRepository
) {
    fun setDriftsmelding(driftsmelding: Driftsmelding) {
        driftsmeldingRepository.lagreDriftsmelding(driftsmelding = driftsmelding)
    }
    fun hentDriftsmeldinger(): List<Driftsmelding> {
        return driftsmeldingRepository.hentAlle()
    }
}
