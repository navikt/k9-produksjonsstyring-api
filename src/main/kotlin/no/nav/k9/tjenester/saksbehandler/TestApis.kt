package no.nav.k9.tjenester.saksbehandler

import io.ktor.application.call
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.withContext
import no.nav.k9.integrasjon.rest.CorrelationId
import no.nav.k9.integrasjon.rest.RequestContextService
import no.nav.k9.tjenester.avdelingsleder.InnloggetNavAnsattDto
import org.slf4j.LoggerFactory
import java.util.*

@KtorExperimentalAPI
@KtorExperimentalLocationsAPI
internal fun Route.TestApis(requestContextService: RequestContextService) {
    @Location("/test")
    class getInnloggetBruker

    val log = LoggerFactory.getLogger("Route.TestApis")
    val gruppenavnSaksbehandler = "0000-GA-k9sak-saksbehandler"
    val gruppenavnVeileder = "0000-GA-k9sak-veileder"
    val gruppenavnBeslutter = "0000-GA-k9sak-beslutter"
    val gruppenavnEgenAnsatt = "0000-GA-GOSYS_UTVIDET"
    val gruppenavnKode6 = "0000-GA-GOSYS_KODE6"
    val gruppenavnKode7 = "0000-GA-GOSYS_KODE7"
    val gruppenavnOppgavestyrer = "0000-GA-k9sak-Oppgavestyrer"


    get { _: getInnloggetBruker ->
        val idtoken = call.idToken()
        withContext(
            requestContextService.getCoroutineContext(
                context = coroutineContext,
                correlationId = CorrelationId(UUID.randomUUID().toString()),//call.correlationId(),
                idToken = idtoken
            )
        ) {
            log.info("id " + idtoken.ident.value)
            log.info("token " + idtoken.value)
        }


//        val id = requestContextService.getIdToken().getId()
//        val subject1 = requestContextService.getIdToken().getSubject()
//        log.info("id" + id)
//        log.info("subject1" + id)
//        val ident = SubjectHandler.getSubjectHandler().uid
//        val ldapBruker = LdapBrukeroppslag().hentBrukerinformasjon(ident)
//        val grupper = LdapUtil().filtrerGrupper(ldapBruker.groups)
//        val innloggetAnsatt = InnloggetNavAnsattDto(
//            ident,
//            ldapBruker.displayName,
//            grupper.contains(gruppenavnSaksbehandler),
//            grupper.contains(gruppenavnVeileder),
//            grupper.contains(gruppenavnBeslutter),
//            grupper.contains(gruppenavnEgenAnsatt),
//            grupper.contains(gruppenavnKode6),
//            grupper.contains(gruppenavnKode7),
//            grupper.contains(gruppenavnOppgavestyrer)
//        )
        call.respond(
            InnloggetNavAnsattDto(
                "alexaban",
                "Saksbehandler Sara",
                kanSaksbehandle = true,
                kanVeilede = true,
                kanBeslutte = true,
                kanBehandleKodeEgenAnsatt = true,
                kanBehandleKode6 = true,
                kanBehandleKode7 = true,
                kanOppgavestyre = true
            )
        )
    }
//    }
}