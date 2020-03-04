package no.nav.k9.domene.repository

import java.util.Optional


import no.nav.k9.domene.organisasjon.Avdeling
import no.nav.k9.domene.organisasjon.Saksbehandler

interface OrganisasjonRepository {

    fun hentAvdelingensSaksbehandlere(avdelingEnhet: String): List<Saksbehandler>

    fun lagre(saksbehandler: Saksbehandler)

    fun slettSaksbehandler(saksbehandlerIdent: String)

    fun hentSaksbehandler(saksbehandlerIdent: String): Saksbehandler

    fun hentAvdeling(avdelingId: Long?): Avdeling

    fun hentAvdelingFraEnhet(avdelingEnhet: String): Avdeling

    fun hentMuligSaksbehandler(saksbehandlerIdent: String): Optional<Saksbehandler>

    fun lagre(avdeling: Avdeling)

    fun refresh(avdeling: Avdeling)

    fun hentAvdelinger(): List<Avdeling>

    fun hentAlleSaksbehandlere(): List<Saksbehandler>

}
