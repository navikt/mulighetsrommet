package no.nav.mulighetsrommet.api.lagretfilter

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import org.koin.ktor.ext.inject
import java.util.*

fun Route.lagretFilterRoutes() {
    val lagretFilterService: LagretFilterService by inject()

    route("/lagret-filter") {
        get("mine/{dokumenttype}") {
            val navIdent = getNavIdent()
            val dokumenttype = call.parameters.getOrFail("dokumenttype")

            val filter = lagretFilterService.getLagredeFiltereForBruker(
                navIdent.value,
                FilterDokumentType.valueOf(dokumenttype),
            )

            call.respond(filter)
        }

        post {
            val request = call.receive<LagretFilterRequest>()
            lagretFilterService.upsertFilter(request.toLagretFilter(id = request.id, brukerId = getNavIdent()))
            call.respond(HttpStatusCode.Created)
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
    val type: FilterDokumentType,
    val filter: JsonElement,
    val sortOrder: Int,
) {
    fun toLagretFilter(id: UUID?, brukerId: NavIdent): LagretFilterUpsert {
        return LagretFilterUpsert(
            id = id,
            brukerId = brukerId.value,
            navn = navn,
            type = type,
            filter = filter,
            sortOrder = sortOrder,
        )
    }
}
