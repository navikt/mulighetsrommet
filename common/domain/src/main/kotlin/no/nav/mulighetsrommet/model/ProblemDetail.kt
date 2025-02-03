package no.nav.mulighetsrommet.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.serializers.ProblemDetailSerializer

/**
 * Problem object after RFC 9457
 * @property type identifies the problem type
 * @property status (optional) HTTP status code
 * @property title (optional) human-readable summary of the problem type
 * @property detail (optional) explanation specific to this occurrence
 * @property instance (optional) identifies the specific occurrence
 * @property extensions (optional) adds additional properties to the Problem object
 */
@Serializable(with = ProblemDetailSerializer::class)
abstract class ProblemDetail {
    abstract val type: String
    abstract val title: String
    abstract val status: Int
    abstract val detail: String
    abstract val instance: String?
    abstract val extensions: Map<String, Any?>?
}
