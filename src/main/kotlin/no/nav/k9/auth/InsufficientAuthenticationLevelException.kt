package no.nav.k9.auth

class InsufficientAuthenticationLevelException(actualAcr : String, requiredAcr : String) : RuntimeException("Innloggingen er på nivå '$actualAcr'. Denne tjenesten krever '$requiredAcr'")