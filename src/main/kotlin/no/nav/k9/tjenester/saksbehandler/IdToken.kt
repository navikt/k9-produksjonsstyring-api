package no.nav.k9.tjenester.saksbehandler

import com.auth0.jwt.JWT
import io.ktor.application.ApplicationCall
import io.ktor.auth.parseAuthorizationHeader
import no.nav.k9.aksjonspunktbehandling.objectMapper
import no.nav.k9.domene.oppslag.Ident
import java.nio.charset.Charset
import java.util.*

data class IdToken(
    override val value: String,
    override val ident: Ident = Ident(
        JWT.decode(value).subject ?: throw IllegalStateException("Token mangler 'sub' claim.")
    )
) : IIdToken {
    override val jwt = try {
        val split = value.split(".")
        val header = String(Base64.getDecoder().decode(split[0]), Charset.defaultCharset())
        val body = String(Base64.getDecoder().decode(split[1]), Charset.defaultCharset())
        objectMapper().readValue(body, JWTToken::class.java)
    } catch (cause: Throwable) {
        throw IdTokenInvalidFormatException(this, cause)
    }

    override fun getName(): String = jwt.name
    override fun getUsername(): String = jwt.preferredUsername
    override fun kanBehandleKode6(): Boolean = jwt.groups.any { s -> s == "87ea7c87-08a2-43bc-83d6-0bfeee92185d" }
    override fun kanBehandleKode7(): Boolean = jwt.groups.any { s -> s == "69d4a70f-1c83-42a8-8fb8-2f5d54048647" }
    override fun kanBehandleEgneAnsatte(): Boolean = jwt.groups.any { s -> s == "de44052d-b062-4497-89a2-0c85b935b808" }
    override fun erOppgavebehandler(): Boolean = jwt.groups.any { s -> s == "a9f5ef81-4e81-42e8-b368-0273071b64b9" }
}

internal fun ApplicationCall.idToken(): IdToken {
    val jwt = request.parseAuthorizationHeader()?.render() ?: throw IllegalStateException("Token ikke satt")
    return IdToken(jwt.substringAfter("Bearer "))
}