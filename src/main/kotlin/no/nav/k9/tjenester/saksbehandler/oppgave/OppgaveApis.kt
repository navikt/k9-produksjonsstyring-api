package no.nav.k9.tjenester.saksbehandler.oppgave

//import no.nav.k9.integrasjon.dto.SakslisteIdDto
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import io.ktor.util.KtorExperimentalAPI
import kotlin.streams.toList

@KtorExperimentalAPI
@KtorExperimentalLocationsAPI
fun Route.OppgaveApis(
    oppgaveTjeneste: OppgaveTjenesteImpl
) {
    @Location("/oppgaver")
    class hentOppgaver

    get { _: hentOppgaver ->
        val queryParameter = call.request.queryParameters["sakslisteId"]

        /*       call.respond(listOf(Oppgave(736, "789453", "98437", "Enhet", LocalDateTime.now(),
                   LocalDateTime.now(), LocalDate.now(), BehandlingStatus.OPPRETTET, BehandlingType.FORSTEGANGSSOKNAD, FagsakYtelseType.PLEIEPENGER_SYKT_BARN,
                   true, "ewk", null, false, UUID.randomUUID(), null,
                   listOf(OppgaveEgenskap(6476, AndreKriterierType.PAPIRSØKNAD, "BLALSL", true)), false,
                   Aksjonspunkter(mapOf())))) */

        val oppgaveliste = oppgaveTjeneste.hentOppgaver(1L).stream().map { t ->

            OppgaveDto(
                OppgaveStatusDto(false, null, false, null, null, null),
                t.behandlingId,
                t.fagsakSaksnummer,
                "Walter Lemon",
                t.system,
                t.aktorId,
                t.behandlingType,
                t.fagsakYtelseType,
                t.behandlingStatus,
                true,
                t.behandlingOpprettet,
                t.behandlingsfrist,
                t.eksternId
            )

        }.toList()


        call.respond(
            HttpStatusCode.Accepted, oppgaveliste
        )

//        oppgaveTjeneste.oprettOppgave(
//            Oppgave(
//                9878,
//                "666",
//                "089870",
//                "Enhet",
//                LocalDateTime.now(),
//                LocalDateTime.now(),
//                LocalDate.now(),
//                BehandlingStatus.OPPRETTET,
//                BehandlingType.FØRSTEGANGSSØKNAD,
//                FagsakYtelseType.PLEIEPENGER_SYKT_BARN,
//                true,
//                "VL",
//                LocalDateTime.now(),
//                false,
//                UUID.randomUUID(),
//                null,
//                listOf(
//                    OppgaveEgenskap(6476, AndreKriterierType.PAPIRSØKNAD, "BLALSL", true)
//                ),
//                false,
//                Aksjonspunkter(mapOf())
//            )
//        )

//        call.respond(oppgaveTjeneste.hentAlleOppgaver())
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