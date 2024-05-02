package no.nav.mulighetsrommet.api.utils

import io.ktor.server.application.*
import io.ktor.util.pipeline.*
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.domain.dbo.NavAnsattRolle
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetStatus
import no.nav.mulighetsrommet.domain.dto.AvtaleStatus
import no.nav.mulighetsrommet.domain.dto.Avtaletype
import no.nav.mulighetsrommet.domain.dto.NavIdent
import no.nav.mulighetsrommet.notifications.NotificationStatus
import no.nav.mulighetsrommet.utils.toUUID
import java.util.*

data class AvtaleFilter(
    val tiltakstypeIder: List<UUID> = emptyList(),
    val search: String? = null,
    val statuser: List<AvtaleStatus.Enum> = emptyList(),
    val avtaletyper: List<Avtaletype> = emptyList(),
    val navRegioner: List<String> = emptyList(),
    val sortering: String? = null,
    val arrangorIds: List<UUID> = emptyList(),
    val administratorNavIdent: NavIdent? = null,
    val personvernBekreftet: Boolean? = null,
)

data class EksternTiltaksgjennomforingFilter(
    val arrangorOrgnr: List<String> = emptyList(),
)

data class EnhetFilter(
    val statuser: List<NavEnhetStatus>? = null,
    val typer: List<Norg2Type>? = null,
    val overordnetEnhet: String? = null,
)

data class NotificationFilter(
    val status: NotificationStatus? = null,
)

data class NotatFilter(
    val avtaleId: UUID? = null,
    val tiltaksgjennomforingId: UUID? = null,
    val opprettetAv: NavIdent? = null,
    val sortering: String? = "dato-created-asc",
)

fun <T : Any> PipelineContext<T, ApplicationCall>.getNotificationFilter(): NotificationFilter {
    val status = call.request.queryParameters["status"]
    return NotificationFilter(
        status = status?.let { NotificationStatus.valueOf(it) },
    )
}

fun <T : Any> PipelineContext<T, ApplicationCall>.getAvtaleFilter(): AvtaleFilter {
    val tiltakstypeIder = call.parameters.getAll("tiltakstyper")?.map { it.toUUID() } ?: emptyList()
    val search = call.request.queryParameters["search"]
    val statuser = call.parameters.getAll("statuser")
        ?.map { status -> AvtaleStatus.Enum.valueOf(status) }
        ?: emptyList()
    val avtaletyper = call.parameters.getAll("avtaletyper")
        ?.map { type -> Avtaletype.valueOf(type) }
        ?: emptyList()
    val navRegioner = call.parameters.getAll("navRegioner") ?: emptyList()
    val sortering = call.request.queryParameters["sort"]
    val arrangorIds = call.parameters.getAll("arrangorer")?.map { UUID.fromString(it) } ?: emptyList()
    val personvernBekreftet = call.request.queryParameters["personvernBekreftet"]?.let { it == "true" }

    return AvtaleFilter(
        tiltakstypeIder = tiltakstypeIder,
        search = search,
        statuser = statuser,
        avtaletyper = avtaletyper,
        navRegioner = navRegioner,
        sortering = sortering,
        arrangorIds = arrangorIds,
        administratorNavIdent = null,
        personvernBekreftet = personvernBekreftet,
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
