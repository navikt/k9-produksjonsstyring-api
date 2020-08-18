package no.nav.k9.integrasjon.rest

import no.nav.k9.tjenester.saksbehandler.IIdToken
import kotlin.coroutines.CoroutineContext

interface IRequestContextService {
    fun getCoroutineContext(
        context: CoroutineContext,
        idToken: IIdToken
    ): CoroutineContext

    fun getIdToken(): IIdToken
    fun getRequestContext(): RequestContextService.RequestContext
}