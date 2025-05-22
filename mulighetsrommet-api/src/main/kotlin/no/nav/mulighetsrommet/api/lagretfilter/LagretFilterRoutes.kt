package no.nav.mulighetsrommet.api.lagretfilter

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.ktor.exception.Forbidden
import no.nav.mulighetsrommet.ktor.plugins.respondWithProblemDetail
import no.nav.mulighetsrommet.model.ProblemDetail
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import org.koin.ktor.ext.inject
import java.util.*

fun Route.lagretFilterRoutes() {
    val lagretFilterService: LagretFilterService by inject()

    route("/lagret-filter") {
        get("mine/{dokumenttype}") {
            val navIdent = getNavIdent()
            val dokumenttype: String by call.parameters

            val filter = lagretFilterService.getLagredeFiltereForBruker(
                brukerId = navIdent.value,
                dokumentType = FilterDokumentType.valueOf(dokumenttype),
            )

            call.respond(filter)
        }

        post {
            val navIdent = getNavIdent()
            val request = call.receive<LagretFilterRequest>()

            lagretFilterService.upsertFilter(brukerId = navIdent.value, request)
                .onLeft {
                    call.respondWithProblemDetail(toProblemDetail(it))
                }
                .onRight {
                    call.respond(HttpStatusCode.OK)
                }
        }

        delete("{id}") {
            val navIdent = getNavIdent()
            val id: UUID by call.parameters

            lagretFilterService.deleteFilter(brukerId = navIdent.value, id)
                .onLeft {
                    call.respondWithProblemDetail(toProblemDetail(it))
                }
                .onRight { filterId ->
                    val status = if (filterId == null) HttpStatusCode.NoContent else HttpStatusCode.OK
                    call.respond(status)
                }
        }
    }
}

private fun toProblemDetail(error: LagretFilterError): ProblemDetail = when (error) {
    is LagretFilterError.Forbidden -> Forbidden(error.message)
}

@Serializable
data class LagretFilterRequest(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val navn: String,
    val type: FilterDokumentType,
    val filter: JsonElement,
    val isDefault: Boolean? = null,
    val sortOrder: Int,
)
