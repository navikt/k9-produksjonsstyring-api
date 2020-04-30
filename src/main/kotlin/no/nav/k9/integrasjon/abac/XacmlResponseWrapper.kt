package no.nav.k9.integrasjon.abac

import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName

private val gson = GsonBuilder().create()

enum class Decision {
    Permit,
    Deny,
    NotApplicable,
    Indeterminate;
}

private data class AbacResponse(
        @SerializedName("Response") val response: Result
)

private data class Result(
    @SerializedName("Decision") val decision: Decision,
    @SerializedName("Status") val status: Status?,
    @SerializedName("Obligations") val obligations: ObligationOrAdvice?,
    @SerializedName("AssociatedAdvice") val associatedAdvice: ObligationOrAdvice?,
    @SerializedName("PolicyIdentifierList") val policyIdentifierList: PolicyIdentifier?
)

private data class Status(
        @SerializedName("StatusCode") val statusCode: StatusCode?
)

private data class StatusCode(
        @SerializedName("Value") val value: String
)

private data class ObligationOrAdvice(
        @SerializedName("Id") val id: String,
        @SerializedName("AttributeAssignment") val attributeAssignment: List<AttributeAssignment>?
)

private data class AttributeAssignment(
        @SerializedName("AttributeId") val attributeId: String,
        @SerializedName("Value") val value: String,
        @SerializedName("Issuer") val issuer: String?,
        @SerializedName("DataType") val dataType: String?,
        @SerializedName("Category") val category: String?
)

private data class PolicyIdentifier(
    @SerializedName("PolicyIdReference") val policyIdReference: List<IdReference>?,
    @SerializedName("PolicySetIdReference") val policySetIdReference: List<IdReference>?
)

private data class IdReference(
        @SerializedName("Id") val id: String,
        @SerializedName("Version") val version: String?
)

class XacmlResponseWrapper(xacmlResponse: String) {
    private var result: Result

    init {
        val responseResult: AbacResponse = gson.fromJson(xacmlResponse, AbacResponse::class.java)
        result = responseResult.response
    }

    fun getDecision(): Decision = result.decision

    fun getStatusLogLine(): String = "ABAC ansvered with status ${result.status?.statusCode?.value}"

    fun getNumberOfObligations(): Int = if (result.obligations != null) 1 else 0

    fun getOblogationsLogLine(): String = "ABAC answered with ${getNumberOfObligations()} obligations"

    fun getNumberOfAdvice(): Int = if (result.associatedAdvice != null) 1 else 0

    fun getAdviceLogLine(): String = "ABAC answered with ${getNumberOfAdvice()} advice"
}