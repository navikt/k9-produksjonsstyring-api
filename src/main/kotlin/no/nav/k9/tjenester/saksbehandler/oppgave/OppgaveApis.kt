package no.nav.k9.tjenester.saksbehandler.oppgave

import io.ktor.application.call
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.locations.post
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.util.KtorExperimentalAPI
import joptsimple.internal.Strings
import kotlinx.coroutines.withContext
import no.nav.k9.Configuration
import no.nav.k9.integrasjon.abac.PepClient
import no.nav.k9.integrasjon.pdl.PdlService
import no.nav.k9.integrasjon.rest.RequestContextService
import no.nav.k9.tjenester.mock.Aksjonspunkter
import no.nav.k9.tjenester.saksbehandler.idToken
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.streams.toList

private val logger: Logger = LoggerFactory.getLogger("nav.OppgaveApis")

@KtorExperimentalAPI
@KtorExperimentalLocationsAPI
internal fun Route.OppgaveApis(
    pepClient: PepClient,
    configuration: Configuration,
    requestContextService: RequestContextService,
    oppgaveTjeneste: OppgaveTjeneste,
    pdlService: PdlService
) {

    @Location("/")
    class hentOppgaver

    get { _: hentOppgaver ->
        val queryParameter = call.request.queryParameters["id"]

        if (configuration.erIkkeLokalt) {
            withContext(
                requestContextService.getCoroutineContext(
                    context = coroutineContext,
                    idToken = call.idToken()
                )
            ) {
                if (pepClient.harBasisTilgang(call.idToken())) {
                    val list = mutableListOf<OppgaveDto>()
                    val oppgaver = oppgaveTjeneste.hentOppgaver(UUID.fromString(queryParameter))
                    for (oppgave in oppgaver) {

                        val person = pdlService.person(oppgave.aktorId)
                        if (person == null) {
                            oppgaveTjeneste.settSkjermet(oppgave)
                            continue
                        }
                        val navn = if (configuration.erIDevFss) {
                            "${oppgave.fagsakSaksnummer} " + Strings.join(
                                oppgave.aksjonspunkter.liste.entries.stream().map { t ->
                                    val a = Aksjonspunkter().aksjonspunkter()
                                        .find { aksjonspunkt -> aksjonspunkt.kode == t.key }
                                    "${t.key} ${a?.navn ?: "Ukjent aksjonspunkt"}"
                                }.toList(),
                                ", "
                            )
                        } else {
                            person.data.hentPerson.navn[0].forkortetNavn
                        }


                        list.add(
                            OppgaveDto(
                                OppgaveStatusDto(false, null, false, null, null),
                                oppgave.behandlingId,
                                oppgave.fagsakSaksnummer,
                                navn,
                                oppgave.system,
                                person.data.hentPerson.folkeregisteridentifikator[0].identifikasjonsnummer,
                                oppgave.behandlingType,
                                oppgave.fagsakYtelseType,
                                oppgave.behandlingStatus,
                                true,
                                oppgave.behandlingOpprettet,
                                oppgave.behandlingsfrist,
                                oppgave.eksternId,
                                tilBeslutter = false,
                                utbetalingTilBruker = false,
                                søktGradering = false,
                                selvstendigFrilans = false,
                                registrerPapir = false,
                                kombinert = false
                            )
                        )
                    }
                    call.respond(
                        list
                    )
                } else {
                    mutableListOf<OppgaveDto>()
                }
            }
        } else {
            val list = mutableListOf<OppgaveDto>()
            val oppgaver = oppgaveTjeneste.hentOppgaver(UUID.fromString(queryParameter))
            for (oppgave in oppgaver) {
                list.add(
                    OppgaveDto(
                        OppgaveStatusDto(false, null, false, null, null),
                        oppgave.behandlingId,
                        oppgave.fagsakSaksnummer,
                        "Navn",
                        oppgave.system,
                        oppgave.aktorId,
                        oppgave.behandlingType,
                        oppgave.fagsakYtelseType,
                        oppgave.behandlingStatus,
                        true,
                        oppgave.behandlingOpprettet,
                        oppgave.behandlingsfrist,
                        oppgave.eksternId,
                        tilBeslutter = false,
                        utbetalingTilBruker = false,
                        søktGradering = false,
                        selvstendigFrilans = false,
                        registrerPapir = false,
                        kombinert = false
                    )
                )
            }
            call.respond(
                list
            )
        
        }
    }

    @Location("/resultat")
    class getOppgaverTilBehandling

    get { _: getOppgaverTilBehandling ->
//        val sakslisteId: SakslisteIdDto =
//            ObjectMapper().readValue(call.request.queryParameters["sakslisteId"], SakslisteIdDto::class.java)
//        val nesteOppgaver = oppgaveTjeneste.hentNesteOppgaver(sakslisteId.verdi)

    }

    @Location("/behandlede")
    class getBehandledeOppgaver

    get { _: getBehandledeOppgaver ->

        if (configuration.erIkkeLokalt) {
            val idToken = call.idToken()
            withContext(
                requestContextService.getCoroutineContext(
                    context = coroutineContext,
                    idToken = idToken
                )
            ) {
                call.respond(oppgaveTjeneste.hentSisteReserverteOppgaver(idToken.ident.value))
            }
        } else {
            call.respond(oppgaveTjeneste.hentSisteReserverteOppgaver("alexaban"))
        }
    }

    @Location("/reserverte")
    class getReserverteOppgaver

    get { _: getReserverteOppgaver ->
        if (configuration.erIkkeLokalt) {
            val idToken = call.idToken()
            withContext(
                requestContextService.getCoroutineContext(
                    context = coroutineContext,
                    idToken = idToken
                )
            ) {
                call.respond(oppgaveTjeneste.hentSisteReserverteOppgaver(idToken.ident.value))
            }
        } else {
            call.respond(oppgaveTjeneste.hentSisteReserverteOppgaver("alexaban"))
        }
    }

    @Location("/antall")
    class hentAntallOppgaverForOppgavekø

    get { _: hentAntallOppgaverForOppgavekø ->
        var uuid = call.request.queryParameters["id"]
        if (uuid.isNullOrBlank()) {
            uuid = UUID.randomUUID().toString()
        }
        call.respond(oppgaveTjeneste.hentAntallOppgaver(UUID.fromString(uuid)!!))
    }

    @Location("/reserver")
    class reserverOppgave

    post { _: reserverOppgave ->
        val oppgaveId = call.receive<OppgaveId>()

        if (configuration.erIkkeLokalt) {
            val idToken = call.idToken()
            withContext(
                requestContextService.getCoroutineContext(
                    context = coroutineContext,
                    idToken = idToken
                )
            ) {
                call.respond(oppgaveTjeneste.reserverOppgave(idToken.ident.value, UUID.fromString(oppgaveId.oppgaveId)))
            }
        } else {
            call.respond(oppgaveTjeneste.reserverOppgave("alexaban", UUID.fromString(oppgaveId.oppgaveId)))
        }
    }

    @Location("/opphev")
    class opphevReservasjon

    post { _: opphevReservasjon ->
        val params = call.receive<OpphevReservasjonId>()
        call.respond(oppgaveTjeneste.frigiOppgave(UUID.fromString(params.oppgaveId), params.begrunnelse))
    }

    @Location("/forleng")
    class forlengReservasjon

    post { _: forlengReservasjon ->
        val oppgaveId = call.receive<OppgaveId>()
        call.respond(oppgaveTjeneste.forlengReservasjonPåOppgave(UUID.fromString(oppgaveId.oppgaveId)))
    }

    @Location("/flytt")
    class flyttReservasjon

    post { _: flyttReservasjon ->
        val params = call.receive<FlyttReservasjonId>()
        call.respond(
            oppgaveTjeneste.flyttReservasjon(
                UUID.fromString(params.oppgaveId),
                params.brukernavn,
                params.begrunnelse
            )
        )
    }

    @Location("/oppgaver-for-fagsaker")
    class oppgaverForFagsaker

    get { _: oppgaverForFagsaker ->
        var saker = call.request.queryParameters["saksnummerListe"]
        val saksnummerliste = saker?.split(",") ?: emptyList()

        if (configuration.erIkkeLokalt) {
            withContext(
                requestContextService.getCoroutineContext(
                    context = coroutineContext,
                    idToken = call.idToken()
                )
            ) { call.respond(oppgaveTjeneste.hentOppgaverFraListe(saksnummerliste)) }
        }
    }
}
