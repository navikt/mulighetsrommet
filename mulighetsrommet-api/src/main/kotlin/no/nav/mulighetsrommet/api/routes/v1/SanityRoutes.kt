package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import no.nav.mulighetsrommet.api.domain.dto.SanityResponse
import no.nav.mulighetsrommet.api.plugins.getNavAnsattAzureId
import no.nav.mulighetsrommet.api.plugins.getNorskIdent
import no.nav.mulighetsrommet.api.services.PoaoTilgangService
import no.nav.mulighetsrommet.api.services.VeilederflateService
import no.nav.mulighetsrommet.api.utils.getAccessToken
import no.nav.mulighetsrommet.api.utils.getTiltaksgjennomforingsFilter
import org.koin.ktor.ext.inject

fun Route.sanityRoutes() {
    val veilederflateService: VeilederflateService by inject()
    val poaoTilgangService: PoaoTilgangService by inject()

    route("/api/v1/internal/sanity") {
        get("/innsatsgrupper") {
            poaoTilgangService.verfiyAccessToModia(getNavAnsattAzureId())
            call.respondWithData(veilederflateService.hentInnsatsgrupper().toResponse())
        }

        get("/tiltakstyper") {
            poaoTilgangService.verfiyAccessToModia(getNavAnsattAzureId())
            call.respondWithData(veilederflateService.hentTiltakstyper().toResponse())
        }

        get("/lokasjoner") {
            poaoTilgangService.verifyAccessToUserFromVeileder(getNavAnsattAzureId(), getNorskIdent())
            call.respond(
                veilederflateService.hentLokasjonerForBrukersEnhetOgFylke(
                    getNorskIdent(),
                    call.getAccessToken(),
                ),
            )
        }

        get("/tiltaksgjennomforinger") {
            poaoTilgangService.verifyAccessToUserFromVeileder(getNavAnsattAzureId(), getNorskIdent())
            val result = veilederflateService.hentTiltaksgjennomforingerForBrukerBasertPaEnhetOgFylke(
                getNorskIdent(),
                call.getAccessToken(),
                getTiltaksgjennomforingsFilter(),
            )
            call.respond(result)
        }

        get("/tiltaksgjennomforing/{id}") {
            poaoTilgangService.verifyAccessToUserFromVeileder(getNavAnsattAzureId(), getNorskIdent())
            val id = call.parameters.getOrFail("id")
            val result = veilederflateService.hentTiltaksgjennomforingMedBrukerdata(
                id,
                getNorskIdent(),
                call.getAccessToken(),
            )

            call.respond(result)
        }

        get("/tiltaksgjennomforing/preview/{id}") {
            poaoTilgangService.verfiyAccessToModia(getNavAnsattAzureId())
            val id = call.parameters.getOrFail("id")
            val result = veilederflateService.hentPreviewTiltaksgjennomforing(
                id,
            )
            call.respond(result)
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
