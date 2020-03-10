package no.nav.k9.tilgangskontroll

import no.nav.k9.tilgangskontroll.abac.Decision
import no.nav.k9.tilgangskontroll.rsbac.DecisionEnums
import no.nav.k9.tilgangskontroll.rsbac.PolicyGenerator
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
        val tilgangTilKodeEgenansatt =
            PolicyGenerator<TilgangskontrollContext, String>({ "Saksbehandler (${context.hentSaksbehandlerId()}) har ikke tilgang til $data" }) {
                if (context.harSaksbehandlerRolle("0000-GA-GOSYS_UTVIDET"))
                    DecisionEnums.PERMIT
                else
                    DecisionEnums.DENY
            }

//        @JvmField
//        val tilgangTilKode6 =
//            PolicyGenerator<TilgangskontrollContext, String>({ "Saksbehandler (${context.hentSaksbehandlerId()}) har ikke tilgang til $data" }) {
//                if (arrayOf("6", "SPSF").contains(data)) {
//                    if (context.harSaksbehandlerRolle("0000-GA-GOSYS_KODE6"))
//                        DecisionEnums.PERMIT
//                    else
//                        DecisionEnums.DENY
//                } else {
//                    DecisionEnums.NOT_APPLICABLE
//                }
//            }
//
//        @JvmField
//        val tilgangTilKode7 =
//            PolicyGenerator<TilgangskontrollContext, String>({ "Saksbehandler (${context.hentSaksbehandlerId()}) har ikke tilgang til $data" }) {
//                if (arrayOf("7", "SPSO").contains(data)) {
//                    if (context.harSaksbehandlerRolle("0000-GA-GOSYS_KODE7"))
//                        DecisionEnums.PERMIT
//                    else
//                        DecisionEnums.DENY
//                } else {
//                    DecisionEnums.NOT_APPLICABLE
//                }
//            }

        @JvmField
        val harRolle =
            PolicyGenerator<TilgangskontrollContext, String>({ "Saksbehandler (${context.hentSaksbehandlerId()}) har ikke tilgang til $data" }) {
                if (context.harSaksbehandlerRolle(data))
                    DecisionEnums.PERMIT
                else
                    DecisionEnums.DENY
            }
    }
}

val log = LoggerFactory.getLogger(Tilgangskontroll::class.java)

open class Tilgangskontroll(context: TilgangskontrollContext) : RSBACImpl<TilgangskontrollContext>(
    context, {
        log.error(it)
        ForbiddenException(it)
    })