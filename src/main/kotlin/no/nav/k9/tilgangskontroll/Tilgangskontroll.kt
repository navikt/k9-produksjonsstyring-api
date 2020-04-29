package no.nav.k9.tilgangskontroll

import no.nav.k9.tilgangskontroll.abac.Decision
import no.nav.k9.tilgangskontroll.rsbac.DecisionEnums
import no.nav.k9.tilgangskontroll.rsbac.RSBACImpl
import org.slf4j.LoggerFactory
import javax.ws.rs.ForbiddenException

fun Decision.toDecisionEnum(): DecisionEnums = when {
    this == Decision.Deny -> DecisionEnums.DENY
    this == Decision.Permit -> DecisionEnums.PERMIT
    else -> DecisionEnums.NOT_APPLICABLE
}

class Policies {
    companion object {
       
    }
}

val log = LoggerFactory.getLogger(Tilgangskontroll::class.java)

open class Tilgangskontroll(context: TilgangskontrollContext) : RSBACImpl<TilgangskontrollContext>(
    context, {
        log.error(it)
        ForbiddenException(it)
    })