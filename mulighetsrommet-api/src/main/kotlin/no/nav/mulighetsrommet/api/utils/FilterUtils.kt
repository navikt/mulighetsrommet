package no.nav.mulighetsrommet.api.utils

import io.ktor.server.application.*
import io.ktor.util.pipeline.*
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetStatus
import no.nav.mulighetsrommet.domain.dto.Avtalestatus
import no.nav.mulighetsrommet.domain.dto.Tiltakstypestatus
import no.nav.mulighetsrommet.utils.toUUID
import java.time.LocalDate
import java.util.*

data class TiltakstypeFilter(
    val search: String?,
    val status: Tiltakstypestatus? = null,
    val kategori: Tiltakstypekategori?,
    val dagensDato: LocalDate = LocalDate.now(),
    val sortering: String? = null,
)

data class AvtaleFilter(
    val tiltakstypeId: UUID? = null,
    val search: String? = null,
    val avtalestatus: Avtalestatus? = null,
    val enhet: String? = null,
    val sortering: String? = null,
    val dagensDato: LocalDate = LocalDate.now(),
)

data class EnhetFilter(
    val statuser: List<NavEnhetStatus> = listOf(
        NavEnhetStatus.AKTIV,
        NavEnhetStatus.UNDER_AVVIKLING,
        NavEnhetStatus.UNDER_ETABLERING
    ),
    val tiltakstypeId: String? = null
)

enum class Tiltakstypekategori {
    INDIVIDUELL, GRUPPE
}

fun <T : Any> PipelineContext<T, ApplicationCall>.getTiltakstypeFilter(): TiltakstypeFilter {
    val search = call.request.queryParameters["search"]
    val status =
        call.request.queryParameters["tiltakstypestatus"]?.let { status -> Tiltakstypestatus.valueOf(status) }
    val kategori =
        call.request.queryParameters["tiltakstypekategori"]?.let { kategori -> Tiltakstypekategori.valueOf(kategori) }
    val sortering = call.request.queryParameters["sort"]
    return TiltakstypeFilter(search = search, status = status, kategori = kategori, sortering = sortering)
}

fun <T : Any> PipelineContext<T, ApplicationCall>.getAvtaleFilter(): AvtaleFilter {
    val tiltakstypeId = call.request.queryParameters["tiltakstypeId"]?.toUUID()
    val search = call.request.queryParameters["search"]
    val avtalestatus =
        call.request.queryParameters["avtalestatus"]?.let { status -> Avtalestatus.valueOf(status) }
    val enhet = call.request.queryParameters["enhet"]
    val sortering = call.request.queryParameters["sort"]
    return AvtaleFilter(
        tiltakstypeId = tiltakstypeId,
        search = search,
        avtalestatus = avtalestatus,
        enhet = enhet,
        sortering = sortering
    )
}

fun <T : Any> PipelineContext<T, ApplicationCall>.getEnhetFilter(): EnhetFilter {
    val tiltakstypeId = call.request.queryParameters["tiltakstypeId"]
    return EnhetFilter(tiltakstypeId = tiltakstypeId)
}
