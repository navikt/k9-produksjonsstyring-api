import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonProperty
import com.sun.istack.NotNull
import java.net.URI
import java.net.URISyntaxException
import java.util.*

@JsonAutoDetect(
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE,
    fieldVisibility = JsonAutoDetect.Visibility.ANY
)
class ResourceLink {
    @JsonProperty("href")
    @NotNull
    val href: URI

    /** Link relationship type.  */
    @JsonProperty("rel")
    @NotNull
    val rel: String

    /** Http Method type.  */
    @JsonProperty("type")
    @NotNull
    val type: HttpMethod

    @JsonProperty("requestPayload")
    var requestPayload: Any? = null

    constructor(href: URI, rel: String, type: HttpMethod) {
        this.href = href
        this.rel = rel
        this.type = type
    }

    /** Ctor lager default GET link.  */
    @JvmOverloads
    constructor(
        href: String,
        rel: String,
        type: HttpMethod = HttpMethod.GET
    ) {
        try {
            this.href = URI(href)
        } catch (e: URISyntaxException) {
            throw IllegalArgumentException(e)
        }
        this.rel = rel
        this.type = type
    }

    constructor(
        href: String,
        rel: String,
        type: HttpMethod,
        requestPayload: Any?
    ) : this(href, rel, type) {
        this.requestPayload = requestPayload
    }

    override fun equals(obj: Any?): Boolean {
        if (obj === this) {
            return true
        } else if (obj == null || this.javaClass != obj.javaClass) {
            return false
        }
        val other = obj as ResourceLink
        return (href == other.href
                && rel == other.rel
                && type == other.type)
    }

    override fun hashCode(): Int {
        return Objects.hash(href, rel, type)
    }

    override fun toString(): String {
        return javaClass.simpleName + "<" + type + " " + href + " [" + rel + "]>"
    }

    enum class HttpMethod {
        POST, GET, PUT, PATCH, DELETE
    }

    fun post(href: String, rel: String, requestPayload: Any?): ResourceLink {
        return ResourceLink(href, rel, HttpMethod.POST, requestPayload)
    }

    fun get(href: String, rel: String, requestPayload: Any?): ResourceLink {
        return ResourceLink(href, rel, HttpMethod.GET, requestPayload)
    }
}