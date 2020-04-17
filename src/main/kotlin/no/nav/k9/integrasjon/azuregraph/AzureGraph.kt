//package no.nav.k9.integrasjon.azuregraph
//
//import com.github.kittinunf.fuel.core.Headers
//import com.github.kittinunf.fuel.httpPost
//import com.microsoft.graph.requests.extensions.GraphServiceClient
//import io.ktor.http.HttpHeaders
//import io.ktor.http.Url
//import no.nav.helse.dusseldorf.ktor.client.buildURL
//import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
//import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
//import no.nav.k9.aksjonspunktbehandling.objectMapper
//import no.nav.k9.domene.oppslag.Ident
//import no.nav.k9.integrasjon.rest.NavHeaders
//import no.nav.k9.integrasjon.rest.idToken
//import org.slf4j.Logger
//import org.slf4j.LoggerFactory
//import org.springframework.http.HttpEntity
//import org.springframework.http.HttpMethod
//import org.springframework.http.ResponseEntity
//import org.springframework.util.MultiValueMap
//import org.springframework.web.client.RestClientException
//import org.springframework.web.client.RestTemplate
//import java.net.URI
//import java.util.*
//import kotlin.coroutines.coroutineContext
//
//
//class AzureGraph(
//    baseUrl: URI,
//    accessTokenClient: AccessTokenClient,
//    private val scope: Set<String> = setOf("https://graph.microsoft.com/.default")
//) {
//    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)
//
//    private companion object {
//        private val log: Logger = LoggerFactory.getLogger(AzureGraph::class.java)
//    }
//
//    private val personUrl = Url.buildURL(
//        baseUrl = URI("https://graph.microsoft.com/v1.0/me/memberOf"),
//        pathParts = listOf()
//    ).toString()
//
//    internal suspend fun person(ident: Ident): String {
//        val graphClient = GraphServiceClient.builder().authenticationProvider { request ->
//            request.addHeader(
//                Headers.AUTHORIZATION,
//                "Bearer " + cachedAccessTokenClient.getAccessToken(scope)
//            )
//        }.buildClient()
//        return ""
//    }
//    fun hentAccessToken(): AadAccessToken? {
//
//        val httpRequest = personUrl
//            .httpPost()
//            .body(
//                objectMapper().writeValueAsString(
//                    queryRequest
//                )
//            )
//            .header(
//                "grant_type" to "client_credentials",
//                "client_id" to "configuration.getmsid",
//                "client_secret" to "configuration.getmsSecret",
//                "scope" to "https://graph.microsoft.com/.default",
//                HttpHeaders.ContentType to "application/json"
//            )
//        
//    }
//}
//
//
//
