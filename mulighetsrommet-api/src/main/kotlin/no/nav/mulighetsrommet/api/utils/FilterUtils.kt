package no.nav.mulighetsrommet.api.utils

import io.ktor.server.application.*
import io.ktor.util.pipeline.*

data class TiltakstypeFilter(
    val search: String?,
    val status: Status,
    val kategori: Tiltakstypekategori?,
    val tags: List<String>?,
)

enum class Status {
    AKTIV, PLANLAGT, AVSLUTTET
}

enum class Tiltakstypekategori {
    INDIVIDUELL, GRUPPE
}

fun <T : Any> PipelineContext<T, ApplicationCall>.getTiltakstypeFilter(): TiltakstypeFilter {
    val search = call.request.queryParameters["search"]
    val status =
        call.request.queryParameters["tiltakstypestatus"]?.let { status -> Status.valueOf(status) } ?: Status.AKTIV
    val kategori =
        call.request.queryParameters["tiltakstypekategori"]?.let { kategori -> Tiltakstypekategori.valueOf(kategori) }
    val tags = call.request.queryParameters["tags"]?.let {
        it.split(",").filter { it.isNotBlank() }
    }?.ifEmpty { emptyList() }
    return TiltakstypeFilter(search = search, status = status, kategori = kategori, tags = tags)
}
