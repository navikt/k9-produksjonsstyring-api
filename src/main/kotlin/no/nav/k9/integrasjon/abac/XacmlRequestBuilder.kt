package no.nav.k9.integrasjon.abac

import com.google.gson.annotations.SerializedName

const val ENVIRONMENT_OIDC_TOKEN_BODY = "no.nav.abac.attributter.environment.felles.oidc_token_body"
const val ENVIRONMENT_PEP_ID = "no.nav.abac.attributter.environment.felles.pep_id"
const val RESOURCE_DOMENE = "no.nav.abac.attributter.resource.felles.domene"
const val RESOURCE = "no.nav.abac.attributter.k9.oppgaveno.nav.abac.attributter.resource.felles.resource"
const val RESOURCE_TYPE = "no.nav.abac.attributter.resource.felles.resource_type"
const val SUBJECTID = "urn:oasis:names:tc:xacml:1.0:subject:subject-id"
const val SUBJECT_TYPE = "no.nav.abac.attributter.subject.felles.subjectType"
const val ACTION_ID = "urn:oasis:names:tc:xacml:1.0:action:action-id"
const val AKSJONSPUNKT_TYPE = "no.nav.abac.attributter.resource.k9.sak.aksjonspunkt_type"
const val ANSVARLIG_SAKSBEHANDLER = "no.nav.abac.attributter.resource.k9.sak.ansvarlig_saksbehandler"
const val BEHANDLINGSSTATUS = "no.nav.abac.attributter.resource.k9.sak.behandlingsstatus"
const val SAKSSTATUS = "no.nav.abac.attributter.resource.k9.sak.saksstatus"
const val OPPGAVESTYRER = "no.nav.abac.attributter.k9.oppgavestyring"
const val BASIS_TILGANG = "no.nav.abac.attributter.k9"
const val INTERNBRUKER = "InternBruker"

enum class Category {
    Resource,
    Action,
    Environment,
    AccessSubject,
    RecipientSubject,
    IntermediarySubject,
    Codebase,
    RequestingMachine;
}

data class CategoryAttribute(
    @SerializedName("Attribute") var attributes: List<CAttribute> = ArrayList()
)

data class CAttribute(
        @SerializedName("AttributeId") val attributeId: String,
        @SerializedName("Value") val value: Any
        )

class XacmlRequestBuilder {
    private val requestAttributes = HashMap<Category, CategoryAttribute>()

    fun addResourceAttribute(id: String, value: Any): XacmlRequestBuilder = addAttributeToCategory(Category.Resource, id, value)
    fun addAccessSubjectAttribute(id: String, value: Any): XacmlRequestBuilder = addAttributeToCategory(Category.AccessSubject, id, value)

    fun addEnvironmentAttribute(id: String, value: Any): XacmlRequestBuilder = addAttributeToCategory(Category.Environment, id, value)

    fun addActionAttribute(id: String, value: Any): XacmlRequestBuilder = addAttributeToCategory(Category.Action, id, value)

    fun build(): Map<String, Map<Category, CategoryAttribute>> {
        return mapOf("Request" to requestAttributes)
    }

    private fun addAttributeToCategory(category: Category, id: String, value: Any): XacmlRequestBuilder {
        requestAttributes.getOrPut(category) { CategoryAttribute() }.attributes += CAttribute(id, value)
        return this
    }
}