package no.nav.k9.tjenester.driftsmeldinger

import io.ktor.application.call
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.locations.post
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route

@KtorExperimentalLocationsAPI
fun Route.DriftsmeldingerApis(
    driftsmeldingTjeneste: DriftsmeldingTjeneste
) {
    @Location("/")
    class driftsmelding 
    
    post { _: driftsmelding ->
        val driftsmeldingDto = call.receive<Driftsmelding>()
        driftsmeldingTjeneste.setDriftsmelding(driftsmeldingDto)        
    }
    
    get { _: driftsmelding ->
        call.respond( driftsmeldingTjeneste.hentDriftsmeldinger())
       
    }
}