package no.nav.mulighetsrommet.api.avtale.api

import arrow.core.flatMap
import io.github.smiley4.ktoropenapi.delete
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.patch
import io.github.smiley4.ktoropenapi.post
import io.github.smiley4.ktoropenapi.put
import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.route
import io.ktor.server.util.getValue
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.MrExceptions
import no.nav.mulighetsrommet.api.aarsakerforklaring.AarsakerOgForklaringRequest
import no.nav.mulighetsrommet.api.aarsakerforklaring.validateAarsakerOgForklaring
import no.nav.mulighetsrommet.api.amo.AmoKategoriseringRequest
import no.nav.mulighetsrommet.api.avtale.AvtaleService
import no.nav.mulighetsrommet.api.avtale.mapper.AvtaleDtoMapper
import no.nav.mulighetsrommet.api.avtale.model.AvbrytAvtaleAarsak
import no.nav.mulighetsrommet.api.avtale.model.AvtaleDto
import no.nav.mulighetsrommet.api.avtale.model.AvtaltSatsDto
import no.nav.mulighetsrommet.api.avtale.model.Opsjonsmodell
import no.nav.mulighetsrommet.api.avtale.model.PrismodellRequest
import no.nav.mulighetsrommet.api.avtale.model.toDto
import no.nav.mulighetsrommet.api.endringshistorikk.EndringshistorikkDto
import no.nav.mulighetsrommet.api.navansatt.ktor.authorize
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.parameters.getPaginationParams
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.plugins.pathParameterUuid
import no.nav.mulighetsrommet.api.responses.PaginatedResponse
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.responses.respondWithStatusResponse
import no.nav.mulighetsrommet.api.services.ExcelService
import no.nav.mulighetsrommet.api.tilsagn.model.AvtalteSatser
import no.nav.mulighetsrommet.ktor.plugins.respondWithProblemDetail
import no.nav.mulighetsrommet.model.AvtaleStatusType
import no.nav.mulighetsrommet.model.Avtaletype
import no.nav.mulighetsrommet.model.Faneinnhold
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Personopplysning
import no.nav.mulighetsrommet.model.ProblemDetail
import no.nav.mulighetsrommet.model.SakarkivNummer
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import no.nav.mulighetsrommet.utdanning.db.UtdanningslopDbo
import no.nav.mulighetsrommet.utils.toUUID
import org.koin.ktor.ext.inject
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class OpprettAvtaleRequest(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val detaljer: DetaljerRequest,
    val prismodell: PrismodellRequest,
    val personvern: PersonvernRequest,
    val veilederinformasjon: VeilederinfoRequest,
)

@Serializable
data class DetaljerRequest(
    val navn: String,
    val tiltakskode: Tiltakskode,
    val arrangor: Arrangor?,
    val sakarkivNummer: SakarkivNummer?,
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val sluttDato: LocalDate?,
    val administratorer: List<NavIdent>,
    val avtaletype: Avtaletype,
    val opsjonsmodell: Opsjonsmodell,
    val amoKategorisering: AmoKategoriseringRequest?,
    val utdanningslop: UtdanningslopDbo?,
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
data class PersonvernRequest(
    val personopplysninger: List<Personopplysning>,
    val personvernBekreftet: Boolean,
)

@Serializable
data class VeilederinfoRequest(
    val navEnheter: List<NavEnhetNummer>,
    val beskrivelse: String?,
    val faneinnhold: Faneinnhold?,
)

@Serializable
data class OpprettOpsjonLoggRequest(
    @Serializable(with = LocalDateSerializer::class)
    val nySluttDato: LocalDate? = null,
    val type: Type,
) {
    enum class Type {
        CUSTOM_LENGDE,
        ETT_AAR,
        SKAL_IKKE_UTLOSE_OPSJON,
    }
}

fun Route.avtaleRoutes() {
    val avtaleService: AvtaleService by inject()
    val db: ApiDatabase by inject()

    route("avtaler") {
        authorize(Rolle.AVTALER_SKRIV) {
            put({
                tags = setOf("Avtale")
                operationId = "opprettAvtale"
                request {
                    body<OpprettAvtaleRequest>()
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Avtalen ble opprettet"
                        body<AvtaleDto>()
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
                val navIdent = getNavIdent()
                val request = call.receive<OpprettAvtaleRequest>()

                val result = avtaleService.create(request, navIdent)
                    .mapLeft { ValidationError(errors = it) }
                    .map { AvtaleDtoMapper.fromAvtale(it) }

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

                    val result = avtaleService.registrerOpsjon(id, request, getNavIdent())
                        .mapLeft { ValidationError("Klarte ikke registrere opsjon", it) }
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

                    val result = avtaleService
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
            patch("{id}/detaljer", {
                tags = setOf("Avtale")
                operationId = "upsertDetaljer"
                request {
                    pathParameterUuid("id")
                    body<DetaljerRequest>()
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Oppdatert avtaledetaljer"
                        body<AvtaleDto>()
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
                val navIdent = getNavIdent()
                val id: UUID by call.parameters
                val request = call.receive<DetaljerRequest>()

                val result = avtaleService.upsertDetaljer(id, request, navIdent)
                    .mapLeft { ValidationError(errors = it) }
                    .map { AvtaleDtoMapper.fromAvtale(it) }

                call.respondWithStatusResponse(result)
            }

            patch("{id}/personvern", {
                tags = setOf("Avtale")
                operationId = "upsertPersonvern"
                request {
                    pathParameterUuid("id")
                    body<PersonvernRequest>()
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Oppdatert personvern"
                        body<AvtaleDto>()
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
                val navIdent = getNavIdent()
                val id: UUID by call.parameters
                val request = call.receive<PersonvernRequest>()

                val result = avtaleService.upsertPersonvern(id, request, navIdent)
                    .mapLeft { ValidationError(errors = it) }
                    .map { AvtaleDtoMapper.fromAvtale(it) }

                call.respondWithStatusResponse(result)
            }

            patch("{id}/veilederinfo", {
                tags = setOf("Avtale")
                operationId = "upsertVeilederinfo"
                request {
                    pathParameterUuid("id")
                    body<VeilederinfoRequest>()
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Oppdatert veilederinfo"
                        body<AvtaleDto>()
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
                val navIdent = getNavIdent()
                val id: UUID by call.parameters
                val request = call.receive<VeilederinfoRequest>()

                val result = avtaleService.upsertVeilederinfo(id, request, navIdent)
                    .mapLeft { ValidationError(errors = it) }
                    .map { AvtaleDtoMapper.fromAvtale(it) }

                call.respondWithStatusResponse(result)
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
                        avtaleService.avbrytAvtale(
                            id,
                            avbruttAv = navIdent,
                            tidspunkt = LocalDateTime.now(),
                            aarsakerOgForklaring = request,
                        )
                    }
                    .onLeft { call.respondWithProblemDetail(ValidationError("Klarte ikke avbryte avtale", it)) }
                    .onRight { call.respond(HttpStatusCode.OK) }
            }

            put("{id}/prismodell", {
                tags = setOf("Avtale")
                operationId = "upsertPrismodell"
                request {
                    pathParameterUuid("id")
                    body<PrismodellRequest>()
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Oppdatert avtale"
                        body<AvtaleDto>()
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
                val navIdent = getNavIdent()
                val id: UUID by call.parameters
                val request = call.receive<PrismodellRequest>()

                val result = avtaleService.upsertPrismodell(id, request, navIdent)
                    .mapLeft { ValidationError(errors = it) }

                call.respondWithStatusResponse(result)
            }

            delete("{id}/kontaktperson/{kontaktpersonId}", {
                tags = setOf("Avtale")
                operationId = "frikobleAvtaleKontaktperson"
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

                avtaleService.frikobleKontaktpersonFraAvtale(
                    kontaktpersonId = kontaktpersonId,
                    avtaleId = id,
                    navIdent = navIdent,
                )

                call.respond(HttpStatusCode.OK)
            }
        }

        get({
            tags = setOf("Avtale")
            operationId = "getAvtaler"
            request {
                queryParameter<String>("search")
                queryParameter<List<String>>("tiltakstyper") {
                    explode = true
                }
                queryParameter<List<AvtaleStatusType>>("statuser") {
                    explode = true
                }
                queryParameter<List<Avtaletype>>("avtaletyper") {
                    explode = true
                }
                queryParameter<List<NavEnhetNummer>>("navEnheter") {
                    explode = true
                }
                queryParameter<List<String>>("arrangorer") {
                    explode = true
                }
                queryParameter<Boolean>("personvernBekreftet")
                queryParameter<Boolean>("visMineAvtaler")
                queryParameter<Int>("page")
                queryParameter<Int>("size")
                queryParameter<String>("sort")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Avtaler filtrert på query parameters"
                    body<PaginatedResponse<AvtaleDto>>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val pagination = getPaginationParams()
            val filter = getAvtaleFilter()

            val result = db.session {
                val (totalCount, items) = queries.avtale.getAll(
                    pagination = pagination,
                    tiltakstypeIder = filter.tiltakstypeIder,
                    search = filter.search,
                    statuser = filter.statuser,
                    avtaletyper = filter.avtaletyper,
                    navEnheter = filter.navEnheter,
                    sortering = filter.sortering,
                    arrangorIds = filter.arrangorIds,
                    administratorNavIdent = filter.administratorNavIdent,
                    personvernBekreftet = filter.personvernBekreftet,
                )

                PaginatedResponse.of(pagination, totalCount, items.map { AvtaleDtoMapper.fromAvtale(it) })
            }

            call.respond(result)
        }

        get("/excel", {
            tags = setOf("Avtale")
            operationId = "lastNedAvtalerSomExcel"
            request {
                queryParameter<String>("search")
                queryParameter<List<String>>("tiltakstyper") {
                    explode = true
                }
                queryParameter<List<AvtaleStatusType>>("statuser") {
                    explode = true
                }
                queryParameter<List<Avtaletype>>("avtaletyper") {
                    explode = true
                }
                queryParameter<List<NavEnhetNummer>>("navEnheter") {
                    explode = true
                }
                queryParameter<List<String>>("arrangorer") {
                    explode = true
                }
                queryParameter<Boolean>("personvernBekreftet")
                queryParameter<Boolean>("visMineAvtaler")
                queryParameter<Int>("page")
                queryParameter<Int>("size")
                queryParameter<String>("sort")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Avtaler filtrert på query parameters"
                    body<ByteArray> {
                        mediaTypes(ContentType.Application.Xlsx)
                    }
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val pagination = getPaginationParams()
            val filter = getAvtaleFilter()

            val avtaler = db.session {
                queries.avtale.getAll(
                    pagination = pagination,
                    tiltakstypeIder = filter.tiltakstypeIder,
                    search = filter.search,
                    statuser = filter.statuser,
                    avtaletyper = filter.avtaletyper,
                    navEnheter = filter.navEnheter,
                    sortering = "tiltakstype_navn-ascending",
                    arrangorIds = filter.arrangorIds,
                    administratorNavIdent = filter.administratorNavIdent,
                    personvernBekreftet = filter.personvernBekreftet,
                )
            }

            val file = ExcelService.createExcelFileForAvtale(avtaler.items)

            call.response.header(HttpHeaders.AccessControlExposeHeaders, HttpHeaders.ContentDisposition)
            call.response.header(
                HttpHeaders.ContentDisposition,
                ContentDisposition.Attachment
                    .withParameter(ContentDisposition.Parameters.FileName, "avtaler.xlsx")
                    .toString(),
            )
            call.response.header(HttpHeaders.ContentType, ContentType.Application.Xlsx.toString())

            call.respondFile(file)
        }

        get("{id}", {
            tags = setOf("Avtale")
            operationId = "getAvtale"
            request {
                pathParameterUuid("id")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Avtalen"
                    body<AvtaleDto>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val id: UUID by call.parameters

            avtaleService.get(id)
                ?.let { call.respond(AvtaleDtoMapper.fromAvtale(it)) }
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
                    description = "Mulige handlinger på avtaler for innlogget bruker"
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
            val ansatt = db.session { queries.ansatt.getByNavIdent(navIdent) }
                ?: throw MrExceptions.navAnsattNotFound(navIdent)

            avtaleService.get(id)
                ?.let { call.respond(avtaleService.handlinger(it, ansatt)) }
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

            val avtale = avtaleService.get(id)
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
            val historikk = avtaleService.getEndringshistorikk(id)
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
        ?.map { status -> AvtaleStatusType.valueOf(status) }
        ?: emptyList()
    val avtaletyper = call.parameters.getAll("avtaletyper")
        ?.map { type -> Avtaletype.valueOf(type) }
        ?: emptyList()
    val navEnheter = call.parameters.getAll("navEnheter")?.map { NavEnhetNummer(it) } ?: emptyList()
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
        navEnheter = navEnheter,
        sortering = sortering,
        arrangorIds = arrangorIds,
        administratorNavIdent = administratorNavIdent,
        personvernBekreftet = personvernBekreftet,
    )
}

data class AvtaleFilter(
    val tiltakstypeIder: List<UUID> = emptyList(),
    val search: String? = null,
    val statuser: List<AvtaleStatusType> = emptyList(),
    val avtaletyper: List<Avtaletype> = emptyList(),
    val navEnheter: List<NavEnhetNummer> = emptyList(),
    val sortering: String? = null,
    val arrangorIds: List<UUID> = emptyList(),
    val administratorNavIdent: NavIdent? = null,
    val personvernBekreftet: Boolean? = null,
)
