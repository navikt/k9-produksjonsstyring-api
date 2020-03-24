package no.nav.k9.tjenester.saksbehandler.oppgave

import io.ktor.application.call
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import io.ktor.util.KtorExperimentalAPI
import no.nav.k9.domene.oppslag.Attributt
import no.nav.k9.domene.oppslag.Ident
import no.nav.k9.integrasjon.rest.RequestContextService
import no.nav.k9.integrasjon.tps.TpsProxyV1Gateway

@KtorExperimentalAPI
@KtorExperimentalLocationsAPI
internal fun Route.OppgaveApis(
    requestContextService: RequestContextService,
    oppgaveTjeneste: OppgaveTjenesteImpl,
    tpsProxyV1Gateway: TpsProxyV1Gateway
) {
    @Location("/oppgaver")
    class hentOppgaver

    get { _: hentOppgaver ->
        val queryParameter = call.request.queryParameters["sakslisteId"]
        val list = mutableListOf<OppgaveDto>()
        val oppgaver = oppgaveTjeneste.hentOppgaver(1L)
        for (oppgave in oppgaver) {
            val tpsPerson = tpsProxyV1Gateway.person(
                ident = Ident(oppgave.aktorId),
                attributter = setOf(Attributt.fornavn, Attributt.mellomnavn, Attributt.etternavn)
            )
            list.add(
                OppgaveDto(
                    OppgaveStatusDto(false, null, false, null, null, null),
                    oppgave.behandlingId,
                    oppgave.fagsakSaksnummer,
                    "${tpsPerson?.fornavn ?: ""} ${tpsPerson?.etternavn ?: ""}",
                    oppgave.system,
                    oppgave.aktorId,
                    oppgave.behandlingType,
                    oppgave.fagsakYtelseType,
                    oppgave.behandlingStatus,
                    true,
                    oppgave.behandlingOpprettet,
                    oppgave.behandlingsfrist,
                    oppgave.eksternId
                )
            )
        }
        call.respond(
            list
        )
    }

    @Location("/oppgaver/resultat")
    class getOppgaverTilBehandling

    get { _: getOppgaverTilBehandling ->

        //        val sakslisteId: SakslisteIdDto =
//            ObjectMapper().readValue(call.request.queryParameters["sakslisteId"], SakslisteIdDto::class.java)
//        val nesteOppgaver = oppgaveTjeneste.hentNesteOppgaver(sakslisteId.verdi)

    }

    @Location("/oppgaver/behandlede")
    class getBehandledeOppgaver

    get { _: getBehandledeOppgaver ->
        call.respond(oppgaveTjeneste.hentSisteReserverteOppgaver())
    }

    @Location("/oppgaver/reserverte")
    class getReserverteOppgaver

    get { _: getReserverteOppgaver ->
        call.respond(oppgaveTjeneste.hentSisteReserverteOppgaver())
    }

    @Location("/oppgaver/antall")
    class hentAntallOppgaverForSaksliste

    get { _: hentAntallOppgaverForSaksliste ->
        val queryParameter = call.request.queryParameters["sakslisteId"]
        call.respond(8)
    }

    @Location("/oppgaver/reserver")
    class reserverOppgave

    post { _: Unit ->


    }

}