package no.nav.k9

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.testing.*
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.k9.integrasjon.rest.RequestContextService
import no.nav.k9.integrasjon.rest.idToken
import org.junit.Test
import java.util.*
import kotlin.test.assertEquals

internal class RequestContextServiceTest {

    @Test
    fun `Får hentet token fra request context`() {
        withTestApplication({ testApp() }) {
            handleRequest(HttpMethod.Get, "/med-request-context").apply {
                assertEquals(HttpStatusCode.InternalServerError, response.status())
            }

            handleRequest(HttpMethod.Get, "/med-request-context") {
                addHeader(HttpHeaders.Authorization, authorizationHeader(username = "Erik"))
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("Hei Erik", response.content)
            }
        }
    }

    private fun Application.testApp(
        requestContextService: RequestContextService = RequestContextService(profile = KoinProfile.PROD)) {
        routing {
            route("med-request-context") {
                get {
                    kotlin.runCatching { requestContextService.withRequestContext(call) {
                        coroutineContext.idToken()
                    }}.fold(
                        onSuccess = {
                            call.respondText("Hei ${it.getUsername()}")
                        },
                        onFailure = {
                            call.respond(HttpStatusCode.InternalServerError)
                        }
                    )
                }
            }
        }
    }

    private fun authorizationHeader(username: String) = "${UUID.randomUUID()}".let { uuid -> Azure.V2_0.generateJwt(
        clientId = "test",
        audience = "test",
        overridingClaims = mapOf(
            "aio" to uuid,
            "preferred_username" to username,
            "name" to "Nordmann",
            "groups" to listOf(uuid),
            "tid" to uuid,
            "uti" to uuid,
            "oid" to uuid
        )
    )}.let { "Bearer $it" }
}