package no.nav.k9.tjenester.avdelingsleder.saksliste

import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.locations.post
import io.ktor.routing.Route

@KtorExperimentalLocationsAPI
fun Route.AvdelingslederSakslisteApis(
) {
    @Location("/avdelingsleder/sakslister")
    class hentAvdelingensSakslister

    get { _: hentAvdelingensSakslister ->
    }

    class opprettNySaksliste

    post { _: opprettNySaksliste ->
    }

    @Location("/avdelingsleder/sakslister/slett")
    class slettSaksliste

    post { _: slettSaksliste ->
    }

    @Location("/avdelingsleder/sakslister/navn")
    class lagreNavn

    post { _: lagreNavn ->
    }

    @Location("/avdelingsleder/sakslister/behandlingstype")
    class lagreBehandlingstype

    post { _: lagreBehandlingstype ->
    }

    @Location("/avdelingsleder/sakslister/ytelsetype")
    class lagreFagsakYtelseType

    post { _: lagreFagsakYtelseType ->
    }

    @Location("/avdelingsleder/sakslister/andre-kriterier")
    class lagreAndreKriterierType

    post { _: lagreAndreKriterierType ->
    }

    @Location("/avdelingsleder/sakslister/sortering")
    class lagreSortering

    post { _: lagreSortering ->
    }

    @Location("/avdelingsleder/sakslister/sortering-tidsintervall-dato")
    class lagreSorteringTidsintervallDato

    post { _: lagreSorteringTidsintervallDato ->
    }

    @Location("/avdelingsleder/sakslister/sortering-tidsintervall-dager")
    class lagreSorteringTidsintervallDager

    post { _: lagreSorteringTidsintervallDager ->
    }

    @Location("/avdelingsleder/sakslister/sortering-tidsintervall-type")
    class lagreSorteringTidsintervallValg

    post { _: lagreSorteringTidsintervallValg ->
    }

    @Location("/avdelingsleder/sakslister/saksbehandler")
    class leggSaksbehandlerTilSaksliste

    post { _: leggSaksbehandlerTilSaksliste ->
    }
}