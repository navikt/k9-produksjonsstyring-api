package no.nav.k9.integrasjon.k9

import io.ktor.util.*
import org.slf4j.LoggerFactory

open class K9SakService @KtorExperimentalAPI constructor(){
    val log = LoggerFactory.getLogger("K9SakService")
//    suspend fun hentIdentTilInnloggetBruker() {
//       "/k9/sak/api/behandling/backend-root/refresh"
//        val httpRequest = personUrl
//            .httpPost()
//            .body(
//                query
//            )
//            .header(
//                HttpHeaders.Authorization to "Bearer ${coroutineContext.idToken().value}",
//                NavHeaders.ConsumerToken to cachedAccessTokenClient.getAccessToken(henteNavnScopes)
//                    .asAuthoriationHeader(),
//                HttpHeaders.Accept to "application/json",
//                HttpHeaders.ContentType to "application/json",
//                NavHeaders.Tema to "OMS",
//                NavHeaders.CallId to UUID.randomUUID().toString()
//            )
//
//        val json = Retry.retry(
//            operation = "hent-ident",
//            initialDelay = Duration.ofMillis(200),
//            factor = 2.0,
//            logger = log
//        ) {
//            val (request, _, result) = Operation.monitored(
//                app = "k9-los-api",
//                operation = "hent-ident",
//                resultResolver = { 200 == it.second.statusCode }
//            ) { httpRequest.awaitStringResponseResult() }
//
//            result.fold(
//                { success -> success },
//                { error ->
//                    log.error(
//                        "Error response = '${error.response.body().asString("text/plain")}' fra '${request.url}'"
//                    )
//                    log.error(error.toString())
//                    throw IllegalStateException("Feil ved henting av saksbehandlers id")
//                }
//            )
//        }
//    }
}