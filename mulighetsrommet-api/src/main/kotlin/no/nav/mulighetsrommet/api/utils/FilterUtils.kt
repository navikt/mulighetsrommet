package no.nav.mulighetsrommet.api.utils

import io.ktor.server.application.*
import io.ktor.util.pipeline.*
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.domain.dbo.NavAnsattRolle
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetStatus
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dto.Avtalestatus
import no.nav.mulighetsrommet.domain.dto.Avtaletype
import no.nav.mulighetsrommet.domain.dto.NavIdent
import no.nav.mulighetsrommet.domain.dto.Tiltaksgjennomforingsstatus
import no.nav.mulighetsrommet.domain.dto.Tiltakstypestatus
import no.nav.mulighetsrommet.notifications.NotificationStatus
import no.nav.mulighetsrommet.utils.toUUID
import java.time.LocalDate
import java.util.*

data class TiltakstypeFilter(
    val search: String? = null,
    val statuser: List<Tiltakstypestatus> = emptyList(),
    val kategorier: List<Tiltakstypekategori> = emptyList(),
    val dagensDato: LocalDate = LocalDate.now(),
    val sortering: String? = null,
)

data class AvtaleFilter(
    val tiltakstypeIder: List<UUID> = emptyList(),
    val search: String? = null,
    val statuser: List<Avtalestatus> = emptyList(),
    val avtaletyper: List<Avtaletype> = emptyList(),
    val navRegioner: List<String> = emptyList(),
    val sortering: String? = null,
    val dagensDato: LocalDate = LocalDate.now(),
    val leverandorOrgnr: List<String> = emptyList(),
    val administratorNavIdent: NavIdent? = null,
)

data class AdminTiltaksgjennomforingFilter(
    val search: String? = null,
    val navEnheter: List<String> = emptyList(),
    val tiltakstypeIder: List<UUID> = emptyList(),
    val statuser: List<Tiltaksgjennomforingsstatus> = emptyList(),
    val sortering: String? = null,
    val sluttDatoCutoff: LocalDate? = ArenaMigrering.TiltaksgjennomforingSluttDatoCutoffDate,
    val dagensDato: LocalDate = LocalDate.now(),
    val avtaleId: UUID? = null,
    val arrangorOrgnr: List<String> = emptyList(),
    val administratorNavIdent: NavIdent? = null,
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

data class NotatFilter(
    val avtaleId: UUID? = null,
    val tiltaksgjennomforingId: UUID? = null,
    val opprettetAv: NavIdent? = null,
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
    val search = call.request.queryParameters["search"]
    val statuser =
        call.parameters.getAll("tiltakstypestatuser")?.map { status -> Tiltakstypestatus.valueOf(status) }
    val kategorier =
        call.parameters.getAll("tiltakstypekategorier")?.map { kategori -> Tiltakstypekategori.valueOf(kategori) }
    val sortering = call.request.queryParameters["sort"]
    return TiltakstypeFilter(
        search = search,
        statuser = statuser ?: emptyList(),
        kategorier = kategorier ?: emptyList(),
        sortering = sortering,
    )
}

fun <T : Any> PipelineContext<T, ApplicationCall>.getAvtaleFilter(): AvtaleFilter {
    val tiltakstypeIder = call.parameters.getAll("tiltakstypeIder")?.map { it.toUUID() } ?: emptyList()
    val search = call.request.queryParameters["search"]
    val statuser =
        call.parameters.getAll("statuser")?.map { status -> Avtalestatus.valueOf(status) } ?: emptyList()
    val avtaletyper =
        call.parameters.getAll("avtaletyper")?.map { type -> Avtaletype.valueOf(type) } ?: emptyList()
    val navRegioner = call.parameters.getAll("navRegioner") ?: emptyList()
    val sortering = call.request.queryParameters["sort"]
    val leverandorOrgnr = call.parameters.getAll("leverandorOrgnr") ?: emptyList()

    return AvtaleFilter(
        tiltakstypeIder = tiltakstypeIder,
        search = search,
        statuser = statuser,
        avtaletyper = avtaletyper,
        navRegioner = navRegioner,
        sortering = sortering,
        leverandorOrgnr = leverandorOrgnr,
        administratorNavIdent = null,
    )
}

fun <T : Any> PipelineContext<T, ApplicationCall>.getAdminTiltaksgjennomforingsFilter(): AdminTiltaksgjennomforingFilter {
    val search = call.request.queryParameters["search"]
    val navEnheter = call.parameters.getAll("navEnheter")
    val tiltakstypeIder =
        call.parameters.getAll("tiltakstypeIder")?.map { UUID.fromString(it) }
    val statuser =
        call.parameters.getAll("statuser")?.map { Tiltaksgjennomforingsstatus.valueOf(it) }
    val sortering = call.request.queryParameters["sort"]
    val avtaleId = call.request.queryParameters["avtaleId"]?.let { if (it.isEmpty()) null else UUID.fromString(it) }
    val arrangorOrgnr = call.parameters.getAll("arrangorOrgnr")
    return AdminTiltaksgjennomforingFilter(
        search = search,
        navEnheter = navEnheter ?: emptyList(),
        tiltakstypeIder = tiltakstypeIder ?: emptyList(),
        statuser = statuser ?: emptyList(),
        sortering = sortering,
        avtaleId = avtaleId,
        arrangorOrgnr = arrangorOrgnr ?: emptyList(),
        administratorNavIdent = null,
    )
}

fun <T : Any> PipelineContext<T, ApplicationCall>.getEnhetFilter(): EnhetFilter {
    val statuser = call.parameters.getAll("statuser")
        ?.map { NavEnhetStatus.valueOf(it) }

    val typer = call.parameters.getAll("typer")
        ?.map { Norg2Type.valueOf(it) }

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
