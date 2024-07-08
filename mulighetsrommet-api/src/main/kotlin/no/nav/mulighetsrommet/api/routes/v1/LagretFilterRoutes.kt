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
import org.koin.ktor.ext.inject

fun Route.lagretFilterRoutes() {
    val lagretFilterService: LagretFilterService by inject()
    route("/api/v1/intern/lagret-filter") {
        get("mine/{dokumenttype}") {
            val navIdent = getNavIdent()
            val dokumenttype = call.parameters.getOrFail("dokumenttype")
            call.respond(lagretFilterService.getLagredeFiltereForBruker(navIdent.value, UpsertFilterEntry.FilterDokumentType.valueOf(dokumenttype)))
        }

        post {
            val request = call.receive<LagretFilterRequest>()
            lagretFilterService.upsertFilter(request.toLagretFilter(getNavIdent()))
            call.respond(HttpStatusCode.Created)
        }
    }
}

@Serializable
data class LagretFilterRequest(
    val navn: String,
    val type: UpsertFilterEntry.FilterDokumentType,
    val filter: JsonElement,
    val sortOrder: Int,
) {
    fun toLagretFilter(brukerId: NavIdent): UpsertFilterEntry {
        return UpsertFilterEntry(
            brukerId = brukerId.value,
            navn = navn,
            type = type,
            filter = filter,
            sortOrder = sortOrder,
        )
    }
}
