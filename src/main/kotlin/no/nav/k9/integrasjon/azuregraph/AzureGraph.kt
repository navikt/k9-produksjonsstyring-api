//package no.nav.k9.integrasjon.azuregraph
//
//import com.github.kittinunf.fuel.core.Headers
//import com.microsoft.graph.requests.extensions.GraphServiceClient
//import com.microsoft.graph.requests.extensions.IDirectoryObjectCollectionPage
//import io.ktor.html.each
//import io.ktor.http.Url
//import no.nav.helse.dusseldorf.ktor.client.buildURL
//import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
//import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
//import no.nav.k9.domene.oppslag.Ident
//import org.slf4j.Logger
//import org.slf4j.LoggerFactory
//import java.net.URI
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
//
//        val memberOf: IDirectoryObjectCollectionPage = graphClient.me().memberOf()
//            .buildRequest()
//            .get()
//        memberOf.each{placeholderItem -> placeholderItem. }
//
//        return json
//    }
//
//  
//}
//
//
//
