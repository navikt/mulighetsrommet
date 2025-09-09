package no.nav.mulighetsrommet.api.avtale.api

import arrow.core.flatMap
import io.github.smiley4.ktoropenapi.delete
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.github.smiley4.ktoropenapi.put
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.aarsakerforklaring.AarsakerOgForklaringRequest
import no.nav.mulighetsrommet.api.aarsakerforklaring.validateAarsakerOgForklaring
import no.nav.mulighetsrommet.api.avtale.AvtaleService
import no.nav.mulighetsrommet.api.avtale.model.*
import no.nav.mulighetsrommet.api.endringshistorikk.EndringshistorikkDto
import no.nav.mulighetsrommet.api.navansatt.ktor.authorize
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.parameters.getPaginationParams
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.plugins.pathParameterUuid
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.responses.respondWithStatusResponse
import no.nav.mulighetsrommet.api.services.ExcelService
import no.nav.mulighetsrommet.api.tilsagn.model.AvtalteSatser
import no.nav.mulighetsrommet.ktor.plugins.respondWithProblemDetail
import no.nav.mulighetsrommet.model.*
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import no.nav.mulighetsrommet.utdanning.db.UtdanningslopDbo
import no.nav.mulighetsrommet.utils.toUUID
import org.koin.ktor.ext.inject
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Serializable
data class AvtaleRequest(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val navn: String,
    val tiltakskode: Tiltakskode,
    val arrangor: Arrangor?,
    val avtalenummer: String?,
    val sakarkivNummer: SakarkivNummer?,
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val sluttDato: LocalDate?,
    val administratorer: List<NavIdent>,
    val avtaletype: Avtaletype,
    val navEnheter: List<NavEnhetNummer>,
    val beskrivelse: String?,
    val faneinnhold: Faneinnhold?,
    val personopplysninger: List<Personopplysning>,
    val personvernBekreftet: Boolean,
    val opsjonsmodell: Opsjonsmodell,
    val amoKategorisering: AmoKategorisering?,
    val utdanningslop: UtdanningslopDbo?,
    val prismodell: PrismodellRequest,
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
data class OpprettOpsjonLoggRequest(
    @Serializable(with = LocalDateSerializer::class)
    val nySluttdato: LocalDate?,
    @Serializable(with = LocalDateSerializer::class)
    val forrigeSluttdato: LocalDate?,
    val status: OpsjonLoggStatus,
)

fun Route.avtaleRoutes() {
    val avtaler: AvtaleService by inject()

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
                post({
                    tags = setOf("Avtale")
                    operationId = "registrerOpsjon"
                    request {
                        pathParameterUuid("id")
                        body<OpprettOpsjonLoggRequest>()
                    }
                    response {
                        code(HttpStatusCode.OK) {
                            description = "Opsjon ble registrert"
                        }
                        code(HttpStatusCode.BadRequest) {
                            description = "Valideringsfeil"
                            body<ValidationError>()
                        }
                        default {
                            description = "Problem details"
                            body<ProblemDetail>()
                        }
                    }
                }) {
                    val id: UUID by call.parameters
                    val request = call.receive<OpprettOpsjonLoggRequest>()
                    val userId = getNavIdent()

                    val opsjonLoggEntry = OpsjonLoggEntry(
                        id = UUID.randomUUID(),
                        avtaleId = id,
                        sluttdato = request.nySluttdato,
                        forrigeSluttdato = request.forrigeSluttdato,
                        status = request.status,
                        registretDato = LocalDate.now(),
                        registrertAv = userId,
                    )
                    val result = avtaler.registrerOpsjon(opsjonLoggEntry)
                        .mapLeft { ValidationError("Klarte ikke registrere opsjon", listOf(it)) }
                        .map { HttpStatusCode.OK }

                    call.respondWithStatusResponse(result)
                }

                delete("{opsjonId}", {
                    tags = setOf("Avtale")
                    operationId = "slettOpsjon"
                    request {
                        pathParameterUuid("id")
                        pathParameterUuid("opsjonId")
                    }
                    response {
                        code(HttpStatusCode.OK) {
                            description = "Opsjon ble slettet"
                        }
                        code(HttpStatusCode.BadRequest) {
                            description = "Valideringsfeil"
                            body<ValidationError>()
                        }
                        default {
                            description = "Problem details"
                            body<ProblemDetail>()
                        }
                    }
                }) {
                    val id: UUID by call.parameters
                    val opsjonId: UUID by call.parameters
                    val userId = getNavIdent()

                    val result = avtaler
                        .slettOpsjon(
                            avtaleId = id,
                            opsjonId = opsjonId,
                            slettesAv = userId,
                        )
                        .mapLeft { ValidationError("Klarte ikke slette opsjon", listOf(it)) }
                        .map { HttpStatusCode.OK }

                    call.respondWithStatusResponse(result)
                }
            }

            put("{id}/avbryt", {
                tags = setOf("Avtale")
                operationId = "avbrytAvtale"
                request {
                    pathParameterUuid("id")
                    body<AarsakerOgForklaringRequest<AvbrytAvtaleAarsak>>()
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Avtale ble avbrutt"
                    }
                    code(HttpStatusCode.BadRequest) {
                        description = "Valideringsfeil"
                        body<ValidationError>()
                    }
                    default {
                        description = "Problem details"
                        body<ProblemDetail>()
                    }
                }
            }) {
                val id: UUID by call.parameters
                val navIdent = getNavIdent()
                val request = call.receive<AarsakerOgForklaringRequest<AvbrytAvtaleAarsak>>()

                validateAarsakerOgForklaring(request.aarsaker, request.forklaring)
                    .flatMap {
                        avtaler.avbrytAvtale(
                            id,
                            avbruttAv = navIdent,
                            tidspunkt = LocalDateTime.now(),
                            aarsakerOgForklaring = request,
                        )
                    }
                    .onLeft { call.respondWithProblemDetail(ValidationError("Klarte ikke avbryte avtale", it)) }
                    .onRight { call.respond(HttpStatusCode.OK) }
            }

            put("{id}/prismodell") {
                val navIdent = getNavIdent()
                val id = call.parameters.getOrFail<UUID>("id")
                val request = call.receive<PrismodellRequest>()

                val result = avtaler.upsertPrismodell(id, request, navIdent)
                    .mapLeft { ValidationError(errors = it) }

                call.respondWithStatusResponse(result)
            }

            delete("{id}/kontaktperson/{kontaktpersonId}", {
                tags = setOf("Avtale")
                operationId = "frikobleKontaktperson"
                request {
                    pathParameterUuid("id")
                    pathParameterUuid("kontaktpersonId")
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Kontaktperson ble frikoblet fra gjennomføring"
                    }
                    default {
                        description = "Problem details"
                        body<ProblemDetail>()
                    }
                }
            }) {
                val id: UUID by call.parameters
                val kontaktpersonId: UUID by call.parameters
                val navIdent = getNavIdent()

                avtaler.frikobleKontaktpersonFraAvtale(
                    kontaktpersonId = kontaktpersonId,
                    avtaleId = id,
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

        get("{id}/handlinger", {
            tags = setOf("Avtale")
            operationId = "getAvtaleHandlinger"
            request {
                pathParameterUuid("id")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Avtale-handlinger for innlogget bruker"
                    body<Set<AvtaleHandling>>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val id: UUID by call.parameters
            val navIdent = getNavIdent()

            avtaler.get(id)
                ?.let { call.respond(avtaler.handlinger(it, navIdent)) }
                ?: call.respond(HttpStatusCode.NotFound, "Det finnes ikke noen avtale med id $id")
        }

        get("{id}/satser", {
            tags = setOf("Avtale")
            operationId = "getAvtalteSatser"
            request {
                pathParameterUuid("id")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Avtalte satser for avtale"
                    body<List<AvtaltSatsDto>>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val id: UUID by call.parameters

            val avtale = avtaler.get(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, "Avtale med id $id finnes ikke")

            val satser = AvtalteSatser.getAvtalteSatser(avtale).toDto()

            call.respond(satser)
        }

        get("{id}/historikk", {
            tags = setOf("Avtale")
            operationId = "getAvtaleEndringshistorikk"
            request {
                pathParameterUuid("id")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Avtalens endringshistorikk"
                    body<EndringshistorikkDto>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val id: UUID by call.parameters
            val historikk = avtaler.getEndringshistorikk(id)
            call.respond(historikk)
        }
    }
}

@Serializable
enum class AvtaleHandling {
    REDIGER,
    AVBRYT,
    OPPRETT_GJENNOMFORING,
    DUPLISER,
    REGISTRER_OPSJON,
    OPPDATER_PRIS,
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
    val administratorNavIdent = call.parameters["visMineAvtaler"]
        ?.takeIf { it == "true" }
        ?.let { getNavIdent() }

    return AvtaleFilter(
        tiltakstypeIder = tiltakstypeIder,
        search = search,
        statuser = statuser,
        avtaletyper = avtaletyper,
        navRegioner = navRegioner,
        sortering = sortering,
        arrangorIds = arrangorIds,
        administratorNavIdent = administratorNavIdent,
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
