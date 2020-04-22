package no.nav.k9.tjenester.saksbehandler


import com.fasterxml.jackson.annotation.JsonProperty

data class JWTToken(
    val aio: String, 
    val aud: String, 
    val azp: String, 
    val azpacr: String, 
    val exp: Int, 
    val groups: List<String>,
    val iat: Int, 
    val iss: String, 
    val name: String, 
    val nbf: Int, 
    val oid: String, 
    @JsonProperty("preferred_username")
    val preferredUsername: String, 
    val scp: String, 
    val sub: String, 
    val tid: String, 
    val uti: String, 
    val ver: String 
)