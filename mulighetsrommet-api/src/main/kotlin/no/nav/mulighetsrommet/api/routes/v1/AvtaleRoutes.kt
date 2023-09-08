package no.nav.mulighetsrommet.api.routes.v1

import arrow.core.Either
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.domain.dbo.AvtaleDbo
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.routes.v1.responses.BadRequest
import no.nav.mulighetsrommet.api.routes.v1.responses.StatusResponse
import no.nav.mulighetsrommet.api.routes.v1.responses.respondWithStatusResponse
import no.nav.mulighetsrommet.api.services.AvtaleService
import no.nav.mulighetsrommet.api.services.ExcelService
import no.nav.mulighetsrommet.api.utils.getAvtaleFilter
import no.nav.mulighetsrommet.api.utils.getPaginationParams
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import no.nav.mulighetsrommet.domain.dto.Avtaletype
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import org.koin.ktor.ext.inject
import java.time.LocalDate
import java.util.*

fun Route.avtaleRoutes() {
    val avtaler: AvtaleService by inject()
    val excelService: ExcelService by inject()

    route("/api/v1/internal/avtaler") {
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
            val overstyrtFilter = filter.copy(sortering = "tiltakstype_navn-ascending")
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

        get("{id}/nokkeltall") {
            val id = call.parameters.getOrFail<UUID>("id")

            val nokkeltall = avtaler.getNokkeltallForAvtaleMedId(id)

            call.respond(nokkeltall)
        }

        put {
            val avtaleRequest = call.receive<AvtaleRequest>()

            call.respondWithStatusResponse(avtaler.upsert(avtaleRequest, getNavIdent()))
        }

        put("{id}/avbryt") {
            val id = call.parameters.getOrFail<UUID>("id")
            call.respondWithStatusResponse(avtaler.avbrytAvtale(id))
        }

        delete("{id}") {
            val id = call.parameters.getOrFail<UUID>("id")
            call.respondWithStatusResponse(avtaler.delete(id))
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
    val leverandorOrganisasjonsnummer: String,
    val leverandorUnderenheter: List<String> = emptyList(),
    @Serializable(with = UUIDSerializer::class)
    val leverandorKontaktpersonId: UUID? = null,
    val avtalenummer: String,
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val sluttDato: LocalDate,
    val navRegion: String,
    val url: String,
    val administrator: String,
    val avtaletype: Avtaletype,
    val prisOgBetalingsinformasjon: String? = null,
    val navEnheter: List<String> = emptyList(),
    val opphav: ArenaMigrering.Opphav = ArenaMigrering.Opphav.MR_ADMIN_FLATE,
    val avslutningsstatus: Avslutningsstatus = Avslutningsstatus.IKKE_AVSLUTTET,
) {
    fun toDbo(): StatusResponse<AvtaleDbo> {
        if (!startDato.isBefore(sluttDato)) {
            return Either.Left(BadRequest("Startdato må være før sluttdato"))
        }
        if (navEnheter.isEmpty()) {
            return Either.Left(BadRequest("Minst étt nav kontor må være valgt"))
        }
        if (leverandorUnderenheter.isEmpty()) {
            return Either.Left(BadRequest("Minst én underenhet til leverandøren må være valgt"))
        }

        return Either.Right(
            AvtaleDbo(
                id = id,
                navn = navn,
                avtalenummer = avtalenummer,
                tiltakstypeId = tiltakstypeId,
                leverandorOrganisasjonsnummer = leverandorOrganisasjonsnummer,
                leverandorUnderenheter = leverandorUnderenheter,
                leverandorKontaktpersonId = leverandorKontaktpersonId,
                startDato = startDato,
                sluttDato = sluttDato,
                arenaAnsvarligEnhet = null,
                navRegion = navRegion,
                avtaletype = avtaletype,
                avslutningsstatus = avslutningsstatus,
                antallPlasser = null,
                url = url,
                administratorer = listOf(administrator),
                prisbetingelser = prisOgBetalingsinformasjon,
                navEnheter = navEnheter,
                opphav = opphav,
            ),
        )
    }
}
