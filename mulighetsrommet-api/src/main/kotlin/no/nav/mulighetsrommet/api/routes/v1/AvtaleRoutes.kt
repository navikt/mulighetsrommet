package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.domain.dbo.UtdanningslopDbo
import no.nav.mulighetsrommet.api.domain.dto.FrikobleKontaktpersonRequest
import no.nav.mulighetsrommet.api.domain.dto.OpsjonLoggEntry
import no.nav.mulighetsrommet.api.parameters.getPaginationParams
import no.nav.mulighetsrommet.api.plugins.AuthProvider
import no.nav.mulighetsrommet.api.plugins.authenticate
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.responses.BadRequest
import no.nav.mulighetsrommet.api.responses.respondWithStatusResponse
import no.nav.mulighetsrommet.api.services.AvtaleService
import no.nav.mulighetsrommet.api.services.ExcelService
import no.nav.mulighetsrommet.api.services.OpsjonLoggService
import no.nav.mulighetsrommet.api.utils.getAvtaleFilter
import no.nav.mulighetsrommet.domain.dto.*
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import org.koin.ktor.ext.inject
import java.time.LocalDate
import java.util.*

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
    val websaknummer: Websaknummer?,
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val sluttDato: LocalDate?,
    val administratorer: List<NavIdent>,
    val avtaletype: Avtaletype,
    val prisbetingelser: String?,
    val navEnheter: List<String>,
    val beskrivelse: String?,
    val faneinnhold: Faneinnhold?,
    val personopplysninger: List<Personopplysning>,
    val personvernBekreftet: Boolean,
    val opsjonsmodellData: OpsjonsmodellData?,
    val amoKategorisering: AmoKategorisering?,
    val utdanningslop: UtdanningslopDbo?,
)

@Serializable
data class OpsjonsmodellData(
    @Serializable(with = LocalDateSerializer::class)
    val opsjonMaksVarighet: LocalDate?,
    val opsjonsmodell: Opsjonsmodell?,
    val customOpsjonsmodellNavn: String? = null,
)

@Serializable
enum class Opsjonsmodell {
    TO_PLUSS_EN,
    TO_PLUSS_EN_PLUSS_EN,
    TO_PLUSS_EN_PLUSS_EN_PLUSS_EN,
    ANNET,
    AVTALE_UTEN_OPSJONSMODELL,
    AVTALE_VALGFRI_SLUTTDATO,
}

@Serializable
data class OpsjonLoggRequest(
    @Serializable(with = UUIDSerializer::class)
    val avtaleId: UUID,
    @Serializable(with = LocalDateSerializer::class)
    val nySluttdato: LocalDate?,
    @Serializable(with = LocalDateSerializer::class)
    val forrigeSluttdato: LocalDate?,
    val status: OpsjonsLoggStatus,
) {
    enum class OpsjonsLoggStatus {
        OPSJON_UTLØST,
        SKAL_IKKE_UTLØSE_OPSJON,
        PÅGÅENDE_OPSJONSPROSESS,
    }
}

@Serializable
data class SlettOpsjonLoggRequest(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = UUIDSerializer::class)
    val avtaleId: UUID,
)

fun Route.avtaleRoutes() {
    val avtaler: AvtaleService by inject()
    val opsjonLoggService: OpsjonLoggService by inject()

    route("personopplysninger") {
        get {
            call.respond(
                Personopplysning
                    .entries
                    .sortedBy { it.sortKey }
                    .map { it.toPersonopplysningData() },
            )
        }
    }

    route("avtaler") {
        authenticate(AuthProvider.AZURE_AD_AVTALER_SKRIV) {
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
                val request = call.receive<AvbrytRequest>()
                val response = avtaler.avbrytAvtale(id, navIdent, request.aarsak)
                call.respondWithStatusResponse(response)
            }

            delete("kontaktperson") {
                val request = call.receive<FrikobleKontaktpersonRequest>()
                val navIdent = getNavIdent()
                val response = avtaler.frikobleKontaktpersonFraAvtale(
                    kontaktpersonId = request.kontaktpersonId,
                    avtaleId = request.dokumentId,
                    navIdent = navIdent,
                )
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
            val file = ExcelService.createExcelFileForAvtale(result.data)
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

    route("opsjoner") {
        authenticate(AuthProvider.AZURE_AD_AVTALER_SKRIV) {
            post {
                val request = call.receive<OpsjonLoggRequest>()
                val userId = getNavIdent()
                val opsjonLoggEntry = OpsjonLoggEntry(
                    avtaleId = request.avtaleId,
                    sluttdato = request.nySluttdato,
                    forrigeSluttdato = request.forrigeSluttdato,
                    status = request.status,
                    registrertAv = userId,
                )

                opsjonLoggService.lagreOpsjonLoggEntry(opsjonLoggEntry)
                call.respond(HttpStatusCode.OK)
            }

            delete {
                val request = call.receive<SlettOpsjonLoggRequest>()
                val userId = getNavIdent()
                opsjonLoggService.delete(
                    opsjonLoggEntryId = request.id,
                    avtaleId = request.avtaleId,
                    slettesAv = userId,
                )
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}
