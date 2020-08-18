package no.nav.k9.integrasjon.rest

import kotlinx.coroutines.asContextElement
import no.nav.k9.tjenester.saksbehandler.IIdToken
import no.nav.k9.tjenester.saksbehandler.IdToken


import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

// For bruk i suspending functions
// https://blog.tpersson.io/2018/04/22/emulating-request-scoped-objects-with-kotlin-coroutines/
private class CoroutineRequestContext(
    internal val idToken: IIdToken
) : AbstractCoroutineContextElement(Key) {
    internal companion object Key : CoroutineContext.Key<CoroutineRequestContext>
}

private fun CoroutineContext.requestContext() =
    get(CoroutineRequestContext.Key) ?: throw IllegalStateException("Request Context ikke satt.")

internal fun CoroutineContext.idToken() = requestContext().idToken


// For bruk i non suspending functions
// https://github.com/Kotlin/kotlinx.coroutines/blob/master/docs/coroutine-context-and-dispatchers.md#thread-local-data
class RequestContextService : IRequestContextService {

    private companion object {
        private val requestContexts = ThreadLocal<RequestContext>()
    }

    override fun getCoroutineContext(
        context: CoroutineContext,
        idToken: IIdToken
    ) = context + requestContexts.asContextElement(
        RequestContext(
            idToken
        )
    ) + CoroutineRequestContext(
        idToken
    )

    override fun getRequestContext() = requestContexts.get() ?: throw IllegalStateException("Request Context ikke satt.")
    
    override fun getIdToken() = getRequestContext().idToken

    data class RequestContext(
        val idToken: IIdToken
    )
}

class RequestContextServiceLocal : IRequestContextService {
    private companion object {
        private val requestContexts = ThreadLocal<RequestContextService.RequestContext>()
    }
    override fun getCoroutineContext(
        context: CoroutineContext,
        idToken: IIdToken
    ) = context + requestContexts.asContextElement(
        RequestContextService.RequestContext(
            idToken
        )
    ) + CoroutineRequestContext(
        idToken
    )


    override fun getIdToken(): IdToken {
        TODO("Not yet implemented")
    }

    override fun getRequestContext(): RequestContextService.RequestContext {
        TODO("Not yet implemented")
    }
}