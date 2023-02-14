package no.nav.mulighetsrommet.api.utils

import io.ktor.server.application.*
import io.ktor.util.pipeline.*
import no.nav.mulighetsrommet.domain.dto.Avtalestatus

data class TiltakstypeFilter(
    val search: String?,
    val status: Status,
    val kategori: Tiltakstypekategori?
)

data class AvtaleFilter(
    val search: String? = "",
    val avtalestatus: Avtalestatus,
    val enhet: String?
)

enum class Status {
    AKTIV, PLANLAGT, AVSLUTTET, ALLE
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

fun <T : Any> PipelineContext<T, ApplicationCall>.getAvtaleFilter(): AvtaleFilter {
    val search = call.request.queryParameters["search"]
    val avtalestatus =
        call.request.queryParameters["avtalestatus"]?.let { status -> Avtalestatus.valueOf(status) } ?: Avtalestatus.Aktiv
    val enhet = call.request.queryParameters["enhet"]
    return AvtaleFilter(
        search = search,
        avtalestatus = avtalestatus,
        enhet = enhet
    )
}
