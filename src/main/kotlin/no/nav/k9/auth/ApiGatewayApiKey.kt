package no.nav.k9.auth

data class ApiGatewayApiKey(val value : String, val headerKey : String = "x-nav-apiKey")