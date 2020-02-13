package no.nav.k9.tjenester.avdelingsleder.oppgave

import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.routing.Route

@KtorExperimentalLocationsAPI
fun Route.AvdelingslederOppgaveApis(
) {
    @Location("/avdelingsleder/oppgaver/antall")
    class hentAntallOppgaverForSaksliste

    get { _: hentAntallOppgaverForSaksliste ->
    }

    @Location("/avdelingsleder/oppgaver/avdelingantall")
    class hentAntallOppgaverForAvdeling

    get { _: hentAntallOppgaverForAvdeling ->
    }
}