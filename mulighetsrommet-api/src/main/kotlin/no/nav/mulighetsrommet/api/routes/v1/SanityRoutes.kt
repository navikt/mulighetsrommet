package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.api.domain.dto.SanityResponse
import no.nav.mulighetsrommet.api.plugins.getNavAnsattAzureId
import no.nav.mulighetsrommet.api.plugins.getNorskIdent
import no.nav.mulighetsrommet.api.services.PoaoTilgangService
import no.nav.mulighetsrommet.api.services.VeilederflateSanityService
import no.nav.mulighetsrommet.api.utils.getAccessToken
import no.nav.mulighetsrommet.api.utils.getTiltaksgjennomforingsFilter
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory

val log = LoggerFactory.getLogger("sanityRouteLogger")
fun Route.sanityRoutes() {
    val veilederflateSanityService: VeilederflateSanityService by inject()
    val poaoTilgangService: PoaoTilgangService by inject()

    route("/api/v1/internal/sanity") {
        get("/innsatsgrupper") {
            poaoTilgangService.verfiyAccessToModia(getNavAnsattAzureId())
            call.respondWithData(veilederflateSanityService.hentInnsatsgrupper().toResponse())
        }

        get("/tiltakstyper") {
            poaoTilgangService.verfiyAccessToModia(getNavAnsattAzureId())
            call.respondWithData(veilederflateSanityService.hentTiltakstyper().toResponse())
        }

        get("/lokasjoner") {
            poaoTilgangService.verfiyAccessToModia(getNavAnsattAzureId())
            call.respondWithData(
                veilederflateSanityService.hentLokasjonerForBrukersEnhetOgFylke(
                    getNorskIdent(),
                    call.getAccessToken(),
                ).toResponse(),
            )
        }

        get("/tiltaksgjennomforinger") {
            poaoTilgangService.verfiyAccessToModia(getNavAnsattAzureId())
            call.respondWithData(
                veilederflateSanityService.hentTiltaksgjennomforingerForBrukerBasertPaEnhetOgFylke(
                    getNorskIdent(),
                    call.getAccessToken(),
                    getTiltaksgjennomforingsFilter(),
                ).toResponse(),
            )
        }

        get("/tiltaksgjennomforing/{id}") {
            poaoTilgangService.verfiyAccessToModia(getNavAnsattAzureId())
            val id = call.parameters["id"] ?: return@get call.respondText(
                text = "Mangler eller ugyldig id",
                status = HttpStatusCode.BadRequest,
            )
            call.respondWithData(veilederflateSanityService.hentTiltaksgjennomforing(id).toResponse())
        }
    }
}

private suspend fun ApplicationCall.respondWithData(apiResponse: ApiResponse) {
    this.response.call.respondText(
        text = apiResponse.text,
        contentType = apiResponse.contentType,
        status = apiResponse.status,
    )
}

private fun SanityResponse.toResponse(): ApiResponse {
    return when (this) {
        is SanityResponse.Result -> ApiResponse(
            text = this.result.toString(),
        )

        is SanityResponse.Error -> {
            log.warn("Error fra Sanity -> {}", this.error)
            return ApiResponse(
                text = this.error.toString(),
                status = HttpStatusCode.InternalServerError,
            )
        }
    }
}

data class ApiResponse(
    val text: String,
    val contentType: ContentType? = ContentType.Application.Json,
    val status: HttpStatusCode? = HttpStatusCode.OK,
)
