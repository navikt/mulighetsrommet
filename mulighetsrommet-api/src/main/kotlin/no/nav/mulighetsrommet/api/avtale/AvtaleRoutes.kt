package no.nav.mulighetsrommet.api.avtale

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.avtale.model.OpsjonLoggEntry
import no.nav.mulighetsrommet.api.gjennomforing.AvbrytRequest
import no.nav.mulighetsrommet.api.navansatt.ktor.authorize
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.parameters.getPaginationParams
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.responses.respondWithStatusResponse
import no.nav.mulighetsrommet.api.services.ExcelService
import no.nav.mulighetsrommet.model.*
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import no.nav.mulighetsrommet.utdanning.db.UtdanningslopDbo
import no.nav.mulighetsrommet.utils.toUUID
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
    val arrangor: Arrangor?,
    val avtalenummer: String?,
    val sakarkivNummer: SakarkivNummer?,
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val sluttDato: LocalDate?,
    val administratorer: List<NavIdent>,
    val avtaletype: Avtaletype,
    val prisbetingelser: String?,
    val navEnheter: List<NavEnhetNummer>,
    val beskrivelse: String?,
    val faneinnhold: Faneinnhold?,
    val personopplysninger: List<Personopplysning>,
    val personvernBekreftet: Boolean,
    val opsjonsmodellData: OpsjonsmodellData?,
    val amoKategorisering: AmoKategorisering?,
    val utdanningslop: UtdanningslopDbo?,
    val prismodell: Prismodell?,
) {

    @Serializable
    data class Arrangor(
        val hovedenhet: Organisasjonsnummer,
        val underenheter: List<Organisasjonsnummer>,
        val kontaktpersoner: List<
            @Serializable(with = UUIDSerializer::class)
            UUID,
            >,
    )
}

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
    @Serializable(with = LocalDateSerializer::class)
    val nySluttdato: LocalDate?,
    @Serializable(with = LocalDateSerializer::class)
    val forrigeSluttdato: LocalDate?,
    val status: OpsjonsLoggStatus,
) {
    enum class OpsjonsLoggStatus {
        OPSJON_UTLOST,
        SKAL_IKKE_UTLOSE_OPSJON,
    }
}

@Serializable
data class SlettOpsjonLoggRequest(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
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
        authorize(Rolle.AVTALER_SKRIV) {
            put {
                val navIdent = getNavIdent()
                val request = call.receive<AvtaleRequest>()

                val result = avtaler.upsert(request, navIdent)
                    .mapLeft { ValidationError(errors = it) }

                call.respondWithStatusResponse(result)
            }

            route("{id}/opsjoner") {
                post {
                    val id: UUID by call.parameters
                    val request = call.receive<OpsjonLoggRequest>()
                    val userId = getNavIdent()

                    val opsjonLoggEntry = OpsjonLoggEntry(
                        avtaleId = id,
                        sluttdato = request.nySluttdato,
                        forrigeSluttdato = request.forrigeSluttdato,
                        status = request.status,
                        registretDato = LocalDate.now(),
                        registrertAv = userId,
                    )
                    opsjonLoggService.lagreOpsjonLoggEntry(opsjonLoggEntry)

                    call.respond(HttpStatusCode.OK)
                }

                delete {
                    val id: UUID by call.parameters
                    val request = call.receive<SlettOpsjonLoggRequest>()
                    val userId = getNavIdent()

                    opsjonLoggService.delete(
                        opsjonLoggEntryId = request.id,
                        avtaleId = id,
                        slettesAv = userId,
                    )

                    call.respond(HttpStatusCode.OK)
                }
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

                avtaler.frikobleKontaktpersonFraAvtale(
                    kontaktpersonId = request.kontaktpersonId,
                    avtaleId = request.dokumentId,
                    navIdent = navIdent,
                )

                call.respond(HttpStatusCode.OK)
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
}

fun RoutingContext.getAvtaleFilter(): AvtaleFilter {
    val tiltakstypeIder = call.parameters.getAll("tiltakstyper")?.map { it.toUUID() } ?: emptyList()
    val search = call.request.queryParameters["search"]
    val statuser = call.parameters.getAll("statuser")
        ?.map { status -> AvtaleStatus.valueOf(status) }
        ?: emptyList()
    val avtaletyper = call.parameters.getAll("avtaletyper")
        ?.map { type -> Avtaletype.valueOf(type) }
        ?: emptyList()
    val navRegioner = call.parameters.getAll("navRegioner")?.map { NavEnhetNummer(it) } ?: emptyList()
    val sortering = call.request.queryParameters["sort"]
    val arrangorIds = call.parameters.getAll("arrangorer")?.map { UUID.fromString(it) } ?: emptyList()
    val personvernBekreftet = call.request.queryParameters["personvernBekreftet"]?.let { it == "true" }

    return AvtaleFilter(
        tiltakstypeIder = tiltakstypeIder,
        search = search,
        statuser = statuser,
        avtaletyper = avtaletyper,
        navRegioner = navRegioner,
        sortering = sortering,
        arrangorIds = arrangorIds,
        administratorNavIdent = null,
        personvernBekreftet = personvernBekreftet,
    )
}

data class AvtaleFilter(
    val tiltakstypeIder: List<UUID> = emptyList(),
    val search: String? = null,
    val statuser: List<AvtaleStatus> = emptyList(),
    val avtaletyper: List<Avtaletype> = emptyList(),
    val navRegioner: List<NavEnhetNummer> = emptyList(),
    val sortering: String? = null,
    val arrangorIds: List<UUID> = emptyList(),
    val administratorNavIdent: NavIdent? = null,
    val personvernBekreftet: Boolean? = null,
)
