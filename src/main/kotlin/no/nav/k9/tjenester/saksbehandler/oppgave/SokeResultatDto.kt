package no.nav.k9.tjenester.saksbehandler.oppgave

import no.nav.k9.tjenester.fagsak.PersonDto

data class SokeResultatDto(
        var ikkeTilgang: Boolean,
        var person: PersonDto?,
        var oppgaver: MutableList<OppgaveDto>
)
