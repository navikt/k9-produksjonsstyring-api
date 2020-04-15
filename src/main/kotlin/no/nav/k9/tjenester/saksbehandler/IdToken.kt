package no.nav.k9.tjenester.saksbehandler

import com.auth0.jwt.JWT
import io.ktor.application.ApplicationCall
import io.ktor.auth.parseAuthorizationHeader
import no.nav.k9.aksjonspunktbehandling.objectMapper
import no.nav.k9.domene.oppslag.Ident
import java.nio.charset.Charset
import java.util.*

data class IdToken(
    internal val value: String,
    internal val ident: Ident = Ident(
        JWT.decode(value).subject ?: throw IllegalStateException("Token mangler 'sub' claim.")
    )
) {
    private val jwt = try {
        val split = value.split(".")
        val header = String(Base64.getDecoder().decode(split[0]), Charset.defaultCharset())
        val body = String(Base64.getDecoder().decode(split[1]), Charset.defaultCharset())
        objectMapper().readValue(body, JWTToken::class.java)
    } catch (cause: Throwable) {
        throw IdTokenInvalidFormatException(this, cause)
    }

    internal fun getName(): String = jwt.name
    internal fun getUsername(): String = jwt.preferredUsername
}

internal fun ApplicationCall.idToken(): IdToken {
    val jwt = request.parseAuthorizationHeader()?.render() ?: throw IllegalStateException("Token ikke satt")
    return IdToken(jwt.substringAfter("Bearer "))
}