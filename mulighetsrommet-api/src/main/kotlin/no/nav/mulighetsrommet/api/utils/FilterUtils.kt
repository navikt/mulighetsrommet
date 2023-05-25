package no.nav.mulighetsrommet.api.utils

import io.ktor.server.application.*
import io.ktor.util.pipeline.*
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetStatus
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dto.Avtalestatus
import no.nav.mulighetsrommet.domain.dto.Tiltaksgjennomforingsstatus
import no.nav.mulighetsrommet.domain.dto.Tiltakstypestatus
import no.nav.mulighetsrommet.notifications.NotificationStatus
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
    val navRegion: String? = null,
    val sortering: String? = null,
    val dagensDato: LocalDate = LocalDate.now(),
)

data class AdminTiltaksgjennomforingFilter(
    val search: String? = null,
    val enhet: String? = null,
    val tiltakstypeId: UUID? = null,
    val status: Tiltaksgjennomforingsstatus? = null,
    val sortering: String? = null,
    val sluttDatoCutoff: LocalDate? = ArenaMigrering.TiltaksgjennomforingSluttDatoCutoffDate,
    val dagensDato: LocalDate = LocalDate.now(),
    val fylkesenhet: String? = null,
    val avtaleId: UUID? = null,
    val organisasjonsnummer: String? = null,
)

data class EnhetFilter(
    val statuser: List<NavEnhetStatus>? = null,
    val tiltakstypeId: UUID? = null,
    val typer: List<Norg2Type>? = null,
)

data class TiltaksgjennomforingFilter(
    val innsatsgruppe: String? = null,
    val tiltakstypeIder: List<String> = emptyList(),
    val sokestreng: String = "",
    val lokasjoner: List<String> = emptyList(),
)

data class NotificationFilter(
    val status: NotificationStatus? = null,
)

enum class Tiltakstypekategori {
    INDIVIDUELL, GRUPPE
}

fun <T : Any> PipelineContext<T, ApplicationCall>.getNotificationFilter(): NotificationFilter {
    val status = call.request.queryParameters["status"]
    return NotificationFilter(
        status = status?.let { NotificationStatus.valueOf(it) },
    )
}

fun <T : Any> PipelineContext<T, ApplicationCall>.getTiltakstypeFilter(): TiltakstypeFilter {
    val search = call.request.queryParameters["search"]
    val status =
        call.request.queryParameters["tiltakstypestatus"]?.let { status -> Tiltakstypestatus.valueOf(status) }
    val kategori =
        call.request.queryParameters["tiltakstypekategori"]?.let { kategori -> Tiltakstypekategori.valueOf(kategori) }
    val sortering = call.request.queryParameters["sort"]
    return TiltakstypeFilter(
        search = search,
        status = status,
        kategori = kategori,
        sortering = sortering,
    )
}

fun <T : Any> PipelineContext<T, ApplicationCall>.getAvtaleFilter(): AvtaleFilter {
    val tiltakstypeId = call.request.queryParameters["tiltakstypeId"]?.toUUID()
    val search = call.request.queryParameters["search"]
    val avtalestatus =
        call.request.queryParameters["avtalestatus"]?.let { status -> Avtalestatus.valueOf(status) }
    val navRegion = call.request.queryParameters["navRegion"]
    val sortering = call.request.queryParameters["sort"]
    return AvtaleFilter(
        tiltakstypeId = tiltakstypeId,
        search = search,
        avtalestatus = avtalestatus,
        navRegion = navRegion,
        sortering = sortering,
    )
}

fun <T : Any> PipelineContext<T, ApplicationCall>.getAdminTiltaksgjennomforingsFilter(): AdminTiltaksgjennomforingFilter {
    val search = call.request.queryParameters["search"]
    val enhet = call.request.queryParameters["enhet"]
    val tiltakstypeId = call.request.queryParameters["tiltakstypeId"]?.let { UUID.fromString(it) }
    val statuser = call.request.queryParameters["status"]?.let { Tiltaksgjennomforingsstatus.valueOf(it) }
    val sortering = call.request.queryParameters["sort"]
    val fylkesenhet = call.request.queryParameters["fylkesenhet"]
    val avtaleId = call.request.queryParameters["avtaleId"]?.let { UUID.fromString(it) }
    return AdminTiltaksgjennomforingFilter(
        search = search,
        enhet = enhet,
        tiltakstypeId = tiltakstypeId,
        status = statuser,
        sortering = sortering,
        fylkesenhet = fylkesenhet,
        avtaleId = avtaleId,
    )
}

fun <T : Any> PipelineContext<T, ApplicationCall>.getEnhetFilter(): EnhetFilter {
    val tiltakstypeId = call.request.queryParameters["tiltakstypeId"]?.toUUID()

    val statuser = call.parameters.getAll("statuser")
        ?.map { NavEnhetStatus.valueOf(it) }
        ?: listOf(
            NavEnhetStatus.AKTIV,
            NavEnhetStatus.UNDER_AVVIKLING,
            NavEnhetStatus.UNDER_ETABLERING,
        )

    val typer = call.parameters.getAll("typer")
        ?.map { Norg2Type.valueOf(it) }
        ?: listOf(
            Norg2Type.FYLKE,
            Norg2Type.LOKAL,
            Norg2Type.ALS,
            Norg2Type.TILTAK,
        )

    return EnhetFilter(
        tiltakstypeId = tiltakstypeId,
        statuser = statuser,
        typer = typer,
    )
}

fun <T : Any> PipelineContext<T, ApplicationCall>.getTiltaksgjennomforingsFilter(): TiltaksgjennomforingFilter {
    val innsatsgruppe = call.parameters["innsatsgruppe"]
    val tiltakstypeIder = call.parameters.getAll("tiltakstypeIder") ?: emptyList()
    val sokestreng = call.parameters["sokestreng"] ?: ""
    val lokasjoner = call.parameters.getAll("lokasjoner") ?: emptyList()
    return TiltaksgjennomforingFilter(
        innsatsgruppe = innsatsgruppe,
        tiltakstypeIder = tiltakstypeIder,
        sokestreng = sokestreng,
        lokasjoner = lokasjoner,
    )
}
