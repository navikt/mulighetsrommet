package no.nav.mulighetsrommet.api.utils

import io.ktor.server.application.*
import io.ktor.util.pipeline.*
import no.nav.mulighetsrommet.api.domain.EnhetStatus
import no.nav.mulighetsrommet.domain.dto.Avtalestatus
import java.time.LocalDate

data class TiltakstypeFilter(
    val search: String?,
    val status: Status? = null,
    val kategori: Tiltakstypekategori?,
    val dagensDato: LocalDate = LocalDate.now()
)

data class AvtaleFilter(
    val search: String?,
    val avtalestatus: Avtalestatus? = null,
    val enhet: String? = null,
    val sortering: String? = null,
    val dagensDato: LocalDate = LocalDate.now()
)

data class EnhetFilter(
    val statuser: List<EnhetStatus> = listOf(
        EnhetStatus.AKTIV,
        EnhetStatus.UNDER_AVVIKLING,
        EnhetStatus.UNDER_ETABLERING
    ),
    val tiltakstypeId: String
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
        call.request.queryParameters["tiltakstypestatus"]?.let { status -> Status.valueOf(status) }
    val kategori =
        call.request.queryParameters["tiltakstypekategori"]?.let { kategori -> Tiltakstypekategori.valueOf(kategori) }
    return TiltakstypeFilter(search, status, kategori)
}

fun <T : Any> PipelineContext<T, ApplicationCall>.getAvtaleFilter(): AvtaleFilter {
    val search = call.request.queryParameters["search"]
    val avtalestatus =
        call.request.queryParameters["avtalestatus"]?.let { status -> Avtalestatus.valueOf(status) }
    val enhet = call.request.queryParameters["enhet"]
    val sortering = call.request.queryParameters["sort"]
    return AvtaleFilter(
        search = search,
        avtalestatus = avtalestatus,
        enhet = enhet,
        sortering = sortering
    )
}

fun <T : Any> PipelineContext<T, ApplicationCall>.getEnhetFilter(): EnhetFilter {
    val tiltakstypeId = call.request.queryParameters["tiltakstypeId"] ?: ""
    return EnhetFilter(tiltakstypeId = tiltakstypeId)
}
