package no.nav.mulighetsrommet.api.tiltakstype.api

import io.ktor.server.routing.RoutingContext
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.application.tiltak.GetAllTiltakstypeKompakt
import no.nav.mulighetsrommet.api.domain.tiltak.SortDirection
import no.nav.mulighetsrommet.api.domain.tiltak.TiltakstypeSortField
import no.nav.mulighetsrommet.model.Faneinnhold
import no.nav.mulighetsrommet.model.TiltakstypeEgenskap
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.UUID

fun RoutingContext.getTiltakstypeKompaktQuery(): GetAllTiltakstypeKompakt {
    val sortField = call.request.queryParameters["sortField"]
        ?.let { runCatching { TiltakstypeSortField.valueOf(it) }.getOrNull() }
        ?: TiltakstypeSortField.NAVN
    val sortDirection = call.request.queryParameters["sortDirection"]
        ?.let { runCatching { SortDirection.valueOf(it) }.getOrNull() }
        ?: SortDirection.ASC
    val egenskaper = call.request.queryParameters.getAll("egenskaper")
        ?.mapNotNull { runCatching { TiltakstypeEgenskap.valueOf(it) }.getOrNull() }
        ?.toSet()
        ?: setOf()
    return GetAllTiltakstypeKompakt(sortField = sortField, sortDirection = sortDirection, egenskaper = egenskaper)
}

@Serializable
data class TiltakstypeDeltakerinfoRequest(
    val ledetekst: String?,
    val innholdskoder: List<String>,
)

@Serializable
data class TiltakstypeVeilederinfoRequest(
    val beskrivelse: String?,
    val faneinnhold: Faneinnhold?,
    val faglenker: List<
        @Serializable(with = UUIDSerializer::class)
        UUID,
        >,
    val kanKombineresMed: List<
        @Serializable(with = UUIDSerializer::class)
        UUID,
        >,
)
