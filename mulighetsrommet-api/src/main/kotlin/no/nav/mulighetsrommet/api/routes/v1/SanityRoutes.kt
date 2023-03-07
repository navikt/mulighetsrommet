package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.api.plugins.getNavAnsattAzureId
import no.nav.mulighetsrommet.api.services.PoaoTilgangService
import no.nav.mulighetsrommet.api.services.SanityResponse
import no.nav.mulighetsrommet.api.services.SanityService
import no.nav.mulighetsrommet.api.utils.getAccessToken
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory

fun Route.sanityRoutes() {
    val log = LoggerFactory.getLogger(this.javaClass)
    val sanityService: SanityService by inject()
    val poaoTilgangService: PoaoTilgangService by inject()

    route("/api/v1/internal/sanity") {
        get {
            poaoTilgangService.verfiyAccessToModia(getNavAnsattAzureId())
            val query = call.request.queryParameters["query"]
                ?: return@get call.respondText("No query parameter with value '?query' present. Cannot execute query against Sanity")
            log.debug("Query sanity with value: $query")
            val fnr = when (call.request.queryParameters["fnr"]) {
                "undefined" -> null // Dersom fnr er 'undefined' så trenger vi ikke verdien og det gjør spørringer mot Sanity raskere
                else -> call.request.queryParameters["fnr"]
            }
            val accessToken = call.getAccessToken()
            call.respondWithData(sanityService.executeQuery(query, fnr, accessToken).toResponse())
        }

        get("/innsatsgrupper") {
            poaoTilgangService.verfiyAccessToModia(getNavAnsattAzureId())
            call.respondWithData(sanityService.hentInnsatsgrupper().toResponse())
        }

        get("/tiltakstyper") {
            poaoTilgangService.verfiyAccessToModia(getNavAnsattAzureId())
            call.respondWithData(sanityService.hentTiltakstyper().toResponse())
        }
    }
}

private suspend fun ApplicationCall.respondWithData(apiResponse: ApiResponse) {
    this.response.call.respondText(
        text = apiResponse.text,
        contentType = apiResponse.contentType,
        status = apiResponse.status
    )
}

private fun SanityResponse.toResponse(): ApiResponse {
    return when (this) {
        is SanityResponse.Result -> ApiResponse(
            text = this.result.toString(),
        )

        is SanityResponse.Error -> ApiResponse(
            text = this.error.toString(),
            status = HttpStatusCode.InternalServerError
        )
    }
}

data class ApiResponse(
    val text: String,
    val contentType: ContentType? = ContentType.Application.Json,
    val status: HttpStatusCode? = HttpStatusCode.OK
)
