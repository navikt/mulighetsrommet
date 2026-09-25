package no.nav.mulighetsrommet.api.clients.saf

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GraphqlRequest<T>(
    val query: String,
    val variables: T,
) {
    @Serializable
    data class JournalpostId(
        val journalpostId: String,
    )
}

@Serializable
data class GraphqlResponse<T>(
    val data: T? = null,
    val errors: List<GraphqlError> = emptyList(),
) {
    @Serializable
    data class GraphqlError(
        val message: String? = null,
        val extensions: Extensions? = null,
    )

    @Serializable
    data class Extensions(
        val code: SafErrorCode? = null,
        val classification: String? = null,
    )
}

@Serializable
data class HentJournalpost(
    val journalpost: SafJournalpost? = null,
)

@Serializable
data class SafJournalpost(
    val journalpostId: String,
    val bruker: SafBruker? = null,
)

@Serializable
data class SafBruker(
    val id: String? = null,
    val type: SafBrukerIdType? = null,
)

enum class SafBrukerIdType {
    AKTOERID,
    FNR,
    ORGNR,
}

enum class SafErrorCode {
    @SerialName("forbidden")
    FORBIDDEN,

    @SerialName("not_found")
    NOT_FOUND,

    @SerialName("bad_request")
    BAD_REQUEST,

    @SerialName("server_error")
    SERVER_ERROR,
}

sealed interface SafError {
    data object NotFound : SafError
    data object Error : SafError
}
