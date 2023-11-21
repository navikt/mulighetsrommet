package no.nav.mulighetsrommet.api.utils

import io.ktor.server.application.*
import io.ktor.server.util.*
import io.ktor.util.pipeline.*
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.domain.dbo.NavAnsattRolle
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetStatus
import no.nav.mulighetsrommet.api.domain.dbo.Utkasttype
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
    val leverandorOrgnr: String? = null,
    val administratorNavIdent: String? = null,
)

data class AdminTiltaksgjennomforingFilter(
    val search: String? = null,
    val navEnhet: String? = null,
    val tiltakstypeId: UUID? = null,
    val status: Tiltaksgjennomforingsstatus? = null,
    val sortering: String? = null,
    val sluttDatoCutoff: LocalDate? = ArenaMigrering.TiltaksgjennomforingSluttDatoCutoffDate,
    val dagensDato: LocalDate = LocalDate.now(),
    val navRegion: String? = null,
    val avtaleId: UUID? = null,
    val arrangorOrgnr: String? = null,
    val administratorNavIdent: String? = null,
)

data class EnhetFilter(
    val statuser: List<NavEnhetStatus>? = null,
    val typer: List<Norg2Type>? = null,
    val overordnetEnhet: String? = null,
)

data class NotificationFilter(
    val status: NotificationStatus? = null,
)

enum class Tiltakstypekategori {
    INDIVIDUELL,
    GRUPPE,
}

data class VirksomhetFilter(
    val til: VirksomhetTil? = null,
)

enum class VirksomhetTil {
    AVTALE,
    TILTAKSGJENNOMFORING,
}

data class UtkastFilter(
    val type: Utkasttype,
    val opprettetAv: String?,
    val avtaleId: UUID?,
)

data class NotatFilter(
    val avtaleId: UUID? = null,
    val tiltaksgjennomforingId: UUID? = null,
    val opprettetAv: String? = null,
    val sortering: String? = "dato-created-asc",
)

fun <T : Any> PipelineContext<T, ApplicationCall>.getVirksomhetFilter(): VirksomhetFilter {
    val til = call.request.queryParameters["til"]
    return VirksomhetFilter(
        til = til?.let { VirksomhetTil.valueOf(it) },
    )
}

fun <T : Any> PipelineContext<T, ApplicationCall>.getNotificationFilter(): NotificationFilter {
    val status = call.request.queryParameters["status"]
    return NotificationFilter(
        status = status?.let { NotificationStatus.valueOf(it) },
    )
}

fun <T : Any> PipelineContext<T, ApplicationCall>.getTiltakstypeFilter(): TiltakstypeFilter {
    val search = call.request.queryParameters["search"]?.nullIfEmpty()
    val status =
        call.request.queryParameters["tiltakstypestatus"]?.let { status -> Tiltakstypestatus.valueOf(status) }
    val kategori =
        call.request.queryParameters["tiltakstypekategori"]?.let { kategori -> Tiltakstypekategori.valueOf(kategori) }
    val sortering = call.request.queryParameters["sort"]?.nullIfEmpty()
    return TiltakstypeFilter(
        search = search,
        status = status,
        kategori = kategori,
        sortering = sortering,
    )
}

fun <T : Any> PipelineContext<T, ApplicationCall>.getAvtaleFilter(): AvtaleFilter {
    val tiltakstypeId = call.request.queryParameters["tiltakstypeId"]?.let { if (it.isEmpty()) null else it.toUUID() }
    val search = call.request.queryParameters["search"]?.nullIfEmpty()
    val avtalestatus =
        call.request.queryParameters["avtalestatus"]?.nullIfEmpty()?.let { status -> Avtalestatus.valueOf(status) }
    val navRegion = call.request.queryParameters["navRegion"]?.nullIfEmpty()
    val sortering = call.request.queryParameters["sort"]?.nullIfEmpty()
    val leverandorOrgnr = call.request.queryParameters["leverandorOrgnr"]?.nullIfEmpty()
    return AvtaleFilter(
        tiltakstypeId = tiltakstypeId,
        search = search,
        avtalestatus = avtalestatus,
        navRegion = navRegion,
        sortering = sortering,
        leverandorOrgnr = leverandorOrgnr,
        administratorNavIdent = null,
    )
}

fun <T : Any> PipelineContext<T, ApplicationCall>.getAdminTiltaksgjennomforingsFilter(): AdminTiltaksgjennomforingFilter {
    val search = call.request.queryParameters["search"]?.nullIfEmpty()
    val navEnhet = call.request.queryParameters["navEnhet"]?.nullIfEmpty()
    val tiltakstypeId =
        call.request.queryParameters["tiltakstypeId"]?.let { if (it.isEmpty()) null else UUID.fromString(it) }
    val statuser =
        call.request.queryParameters["status"]?.nullIfEmpty()?.let { Tiltaksgjennomforingsstatus.valueOf(it) }
    val sortering = call.request.queryParameters["sort"]?.nullIfEmpty()
    val navRegion = call.request.queryParameters["navRegion"]?.nullIfEmpty()
    val avtaleId = call.request.queryParameters["avtaleId"]?.let { if (it.isEmpty()) null else UUID.fromString(it) }
    val arrangorOrgnr = call.request.queryParameters["arrangorOrgnr"]?.nullIfEmpty()
    return AdminTiltaksgjennomforingFilter(
        search = search,
        navEnhet = navEnhet,
        tiltakstypeId = tiltakstypeId,
        status = statuser,
        sortering = sortering,
        navRegion = navRegion,
        avtaleId = avtaleId,
        arrangorOrgnr = arrangorOrgnr,
        administratorNavIdent = null,
    )
}

fun <T : Any> PipelineContext<T, ApplicationCall>.getEnhetFilter(): EnhetFilter {
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
        statuser = statuser,
        typer = typer,
    )
}

data class NavAnsattFilter(
    val roller: List<NavAnsattRolle> = emptyList(),
)

fun <T : Any> PipelineContext<T, ApplicationCall>.getNavAnsattFilter(): NavAnsattFilter {
    val azureIder = call.parameters.getAll("roller")?.map { NavAnsattRolle.valueOf(it) } ?: emptyList()
    return NavAnsattFilter(
        roller = azureIder,
    )
}

fun <T : Any> PipelineContext<T, ApplicationCall>.getUtkastFilter(): UtkastFilter {
    val type = Utkasttype.valueOf(call.request.queryParameters.getOrFail("utkasttype"))
    val avtaleId = call.request.queryParameters["avtaleId"]?.let { if (it.isEmpty()) null else UUID.fromString(it) }
    return UtkastFilter(
        type = type,
        opprettetAv = null,
        avtaleId = avtaleId,
    )
}

fun <T : Any> PipelineContext<T, ApplicationCall>.getNotatFilter(): NotatFilter {
    val avtaleId = call.request.queryParameters["avtaleId"]?.let { if (it.isEmpty()) null else it.toUUID() }
    val tiltaksgjennomforingId =
        call.request.queryParameters["tiltaksgjennomforingId"]?.let { if (it.isEmpty()) null else it.toUUID() }
    val sortering = call.request.queryParameters["order"]

    return NotatFilter(
        avtaleId = avtaleId,
        tiltaksgjennomforingId = tiltaksgjennomforingId,
        opprettetAv = null,
        sortering = sortering,
    )
}

fun String.nullIfEmpty(): String? {
    return this.ifEmpty { null }
}
