package no.nav.mulighetsrommet.api.utils

import io.ktor.server.application.*
import io.ktor.util.pipeline.*

data class TiltakstypeFilter(
    val search: String?,
    val status: Status,
    val kategori: Tiltakstypekategori?
)

enum class Status {
    AKTIV, PLANLAGT, UTFASET
}

enum class Tiltakstypekategori {
    INDIVIDUELL, GRUPPE
}
fun <T : Any> PipelineContext<T, ApplicationCall>.getTiltakstypeFilter(): TiltakstypeFilter {
    val search = call.request.queryParameters["search"]
    val status =
        call.request.queryParameters["tiltakstypestatus"]?.let { status -> Status.valueOf(status) } ?: Status.AKTIV
    val kategori = call.request.queryParameters["tiltakstypekategori"]?.let { kategori -> Tiltakstypekategori.valueOf(kategori) }
    return TiltakstypeFilter(search, status, kategori)
}
