package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.plugins.getNavAnsattAzureId
import no.nav.mulighetsrommet.api.services.PoaoTilgangService
import no.nav.mulighetsrommet.api.services.VeilederflateService
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import no.nav.mulighetsrommet.ktor.extensions.getAccessToken
import org.koin.ktor.ext.inject
import java.util.*

fun Route.sanityRoutes() {
    val veilederflateService: VeilederflateService by inject()
    val poaoTilgangService: PoaoTilgangService by inject()

    route("/api/v1/internal/sanity") {
        get("/innsatsgrupper") {
            poaoTilgangService.verfiyAccessToModia(getNavAnsattAzureId())

            val innsatsgrupper = veilederflateService.hentInnsatsgrupper()

            call.respond(innsatsgrupper)
        }

        get("/tiltakstyper") {
            poaoTilgangService.verfiyAccessToModia(getNavAnsattAzureId())

            val tiltakstyper = veilederflateService.hentTiltakstyper()

            call.respond(tiltakstyper)
        }

        post("/tiltaksgjennomforinger") {
            val request = call.receive<GetRelevanteTiltaksgjennomforingerForBrukerRequest>()

            poaoTilgangService.verifyAccessToUserFromVeileder(getNavAnsattAzureId(), request.norskIdent)

            val result = veilederflateService.hentTiltaksgjennomforingerForBrukerBasertPaEnhetOgFylke(
                request,
                call.getAccessToken(),
            )

            call.respond(result)
        }

        post("/tiltaksgjennomforing") {
            val request = call.receive<GetTiltaksgjennomforingForBrukerRequest>()

            poaoTilgangService.verifyAccessToUserFromVeileder(getNavAnsattAzureId(), request.norskIdent)

            val result = veilederflateService.hentTiltaksgjennomforingMedBrukerdata(
                request,
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

@Serializable
data class GetRelevanteTiltaksgjennomforingerForBrukerRequest(
    val norskIdent: String,
    val innsatsgruppe: String? = null,
    val tiltakstypeIds: List<String> = emptyList(),
    val search: String? = null,
)

@Serializable
data class GetTiltaksgjennomforingForBrukerRequest(
    val norskIdent: String,
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
)
