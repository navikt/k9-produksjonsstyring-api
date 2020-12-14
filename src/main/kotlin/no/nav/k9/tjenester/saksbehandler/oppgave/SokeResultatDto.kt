package no.nav.k9.tjenester.saksbehandler.oppgave

import no.nav.k9.tjenester.fagsak.FagsakDto

data class SokeResultatDto(
        var ikkeTilgang: Boolean,
        val fagsaker: MutableList<FagsakDto>
)
