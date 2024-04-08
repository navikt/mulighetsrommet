package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.plugins.AuthProvider
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.routes.v1.parameters.getPaginationParams
import no.nav.mulighetsrommet.api.routes.v1.responses.BadRequest
import no.nav.mulighetsrommet.api.routes.v1.responses.respondWithStatusResponse
import no.nav.mulighetsrommet.api.services.AvtaleService
import no.nav.mulighetsrommet.api.services.ExcelService
import no.nav.mulighetsrommet.api.utils.getAvtaleFilter
import no.nav.mulighetsrommet.domain.dto.Avtaletype
import no.nav.mulighetsrommet.domain.dto.Faneinnhold
import no.nav.mulighetsrommet.domain.dto.NavIdent
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import org.koin.ktor.ext.inject
import java.time.LocalDate
import java.util.*

fun Route.avtaleRoutes() {
    val avtaler: AvtaleService by inject()
    val excelService: ExcelService by inject()

    route("/api/v1/internal/avtaler") {
        authenticate(AuthProvider.AZURE_AD_AVTALER_SKRIV.name, strategy = AuthenticationStrategy.Required) {
            put {
                val navIdent = getNavIdent()
                val request = call.receive<AvtaleRequest>()

                val result = avtaler.upsert(request, navIdent)
                    .mapLeft { BadRequest(errors = it) }

                call.respondWithStatusResponse(result)
            }

            put("{id}/avbryt") {
                val id = call.parameters.getOrFail<UUID>("id")
                val navIdent = getNavIdent()
                val response = avtaler.avbrytAvtale(id, navIdent)
                call.respondWithStatusResponse(response)
            }
        }

        get {
            val pagination = getPaginationParams()
            val filter = getAvtaleFilter()
            val result = avtaler.getAll(filter, pagination)

            call.respond(result)
        }

        get("mine") {
            val pagination = getPaginationParams()
            val filter = getAvtaleFilter().copy(administratorNavIdent = getNavIdent())
            val result = avtaler.getAll(filter, pagination)

            call.respond(result)
        }

        get("/excel") {
            val pagination = getPaginationParams()
            val filter = getAvtaleFilter()
            val navIdent = call.parameters["visMineAvtaler"]?.let {
                if (it == "true") {
                    getNavIdent()
                } else {
                    null
                }
            }
            val overstyrtFilter = filter.copy(
                sortering = "tiltakstype_navn-ascending",
                administratorNavIdent = navIdent,
            )
            val result = avtaler.getAll(overstyrtFilter, pagination)
            val file = excelService.createExcelFile(result.data)
            call.response.header(
                HttpHeaders.ContentDisposition,
                ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, "avtaler.xlsx")
                    .toString(),
            )
            call.response.header("Access-Control-Expose-Headers", HttpHeaders.ContentDisposition)
            call.response.header(
                HttpHeaders.ContentType,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            )
            call.respondFile(file)
        }

        get("{id}") {
            val id = call.parameters.getOrFail<UUID>("id")

            avtaler.get(id)
                ?.let { call.respond(it) }
                ?: call.respond(HttpStatusCode.NotFound, "Det finnes ikke noen avtale med id $id")
        }

        get("{id}/historikk") {
            val id: UUID by call.parameters
            val historikk = avtaler.getEndringshistorikk(id)
            call.respond(historikk)
        }
    }
}

@Serializable
data class AvtaleRequest(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val navn: String,
    @Serializable(with = UUIDSerializer::class)
    val tiltakstypeId: UUID,
    val arrangorOrganisasjonsnummer: String,
    val arrangorUnderenheter: List<String>,
    val arrangorKontaktpersoner: List<
        @Serializable(with = UUIDSerializer::class)
        UUID,
        >,
    val avtalenummer: String?,
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val sluttDato: LocalDate?,
    val url: String?,
    val administratorer: List<NavIdent>,
    val avtaletype: Avtaletype,
    val prisbetingelser: String?,
    val navEnheter: List<String>,
    val beskrivelse: String?,
    val faneinnhold: Faneinnhold?,
)
