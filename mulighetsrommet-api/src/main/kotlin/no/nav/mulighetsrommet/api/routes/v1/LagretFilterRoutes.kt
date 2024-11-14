package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.services.LagretFilterService
import no.nav.mulighetsrommet.api.services.UpsertFilterEntry
import no.nav.mulighetsrommet.domain.dto.NavIdent
import no.nav.mulighetsrommet.domain.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import org.koin.ktor.ext.inject
import java.time.LocalDateTime
import java.util.*

fun Route.lagretFilterRoutes() {
    val lagretFilterService: LagretFilterService by inject()

    route("/lagret-filter") {
        get("mine/{dokumenttype}") {
            val navIdent = getNavIdent()
            val dokumenttype = call.parameters.getOrFail("dokumenttype")
            lagretFilterService.getLagredeFiltereForBruker(
                navIdent.value,
                UpsertFilterEntry.FilterDokumentType.valueOf(dokumenttype),
            )
                .onRight {
                    call.respond(it)
                }.onLeft {
                    call.respondText("Klarte ikke hente lagrede filter", status = HttpStatusCode.InternalServerError)
                }
        }

        put("mine/{dokumenttype}") {
            val navIdent = getNavIdent()
            val dokumenttype = call.parameters.getOrFail("dokumenttype")
            lagretFilterService.clearSistBruktTimestampForBruker(brukerId = navIdent.value, dokumentType = UpsertFilterEntry.FilterDokumentType.valueOf(dokumenttype))
            call.respond(HttpStatusCode.OK)
        }

        post {
            val request = call.receive<LagretFilterRequest>()
            lagretFilterService.upsertFilter(request.toLagretFilter(id = request.id, brukerId = getNavIdent()))
            call.respond(HttpStatusCode.Created)
        }

        put("{id}") {
            val id = call.parameters.getOrFail<UUID>("id")
            lagretFilterService.updateSistBruktTimestamp(id)
            call.respond(HttpStatusCode.OK)
        }

        delete("{id}") {
            val id = call.parameters.getOrFail("id")
            lagretFilterService.deleteFilter(UUID.fromString(id))
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

@Serializable
data class LagretFilterRequest(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val navn: String,
    val type: UpsertFilterEntry.FilterDokumentType,
    val filter: JsonElement,
    val sortOrder: Int,
    @Serializable(with = LocalDateTimeSerializer::class)
    val sistBrukt: LocalDateTime?,
) {
    fun toLagretFilter(id: UUID?, brukerId: NavIdent): UpsertFilterEntry {
        return UpsertFilterEntry(
            id = id,
            brukerId = brukerId.value,
            navn = navn,
            type = type,
            filter = filter,
            sortOrder = sortOrder,
            sistBrukt = sistBrukt,
        )
    }
}
