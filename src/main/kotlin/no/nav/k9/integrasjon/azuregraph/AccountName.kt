package no.nav.k9.integrasjon.azuregraph


import com.fasterxml.jackson.annotation.JsonProperty

data class AccountName(
    @JsonProperty("@odata.context")
    val odataContext: String,
    val onPremisesSamAccountName: String
)