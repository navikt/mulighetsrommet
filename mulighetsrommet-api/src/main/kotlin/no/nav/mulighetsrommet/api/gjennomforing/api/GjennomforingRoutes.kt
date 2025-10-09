package no.nav.mulighetsrommet.api.gjennomforing.api

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.flatMap
import arrow.core.nel
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import arrow.core.raise.zipOrAccumulate
import io.github.smiley4.ktoropenapi.delete
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.put
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.aarsakerforklaring.AarsakerOgForklaringRequest
import no.nav.mulighetsrommet.api.aarsakerforklaring.validateAarsakerOgForklaring
import no.nav.mulighetsrommet.api.avtale.api.VeilederinfoRequest
import no.nav.mulighetsrommet.api.endringshistorikk.EndringshistorikkDto
import no.nav.mulighetsrommet.api.gjennomforing.mapper.GjennomforingDtoMapper
import no.nav.mulighetsrommet.api.gjennomforing.model.AvbrytGjennomforingAarsak
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingDto
import no.nav.mulighetsrommet.api.gjennomforing.service.GjennomforingService
import no.nav.mulighetsrommet.api.navansatt.ktor.authorize
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.parameters.getPaginationParams
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.plugins.pathParameterUuid
import no.nav.mulighetsrommet.api.plugins.queryParameterUuid
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.responses.PaginatedResponse
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.responses.respondWithStatusResponse
import no.nav.mulighetsrommet.api.services.ExcelService
import no.nav.mulighetsrommet.ktor.plugins.respondWithProblemDetail
import no.nav.mulighetsrommet.model.*
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import no.nav.mulighetsrommet.utdanning.db.UtdanningslopDbo
import org.koin.ktor.ext.inject
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

fun Route.gjennomforingRoutes() {
    val db: ApiDatabase by inject()
    val gjennomforinger: GjennomforingService by inject()

    route("gjennomforinger") {
        authorize(Rolle.TILTAKSGJENNOMFORINGER_SKRIV) {
            put({
                tags = setOf("Gjennomforing")
                operationId = "upsertGjennomforing"
                request {
                    body<GjennomforingRequest>()
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Gjennomføring ble upsertet"
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
                val request = call.receive<GjennomforingRequest>()
                val navIdent = getNavIdent()

                val result = gjennomforinger.upsert(request, navIdent)
                    .mapLeft { ValidationError(errors = it) }
                    .map { GjennomforingDtoMapper.fromGjennomforing(it) }

                call.respondWithStatusResponse(result)
            }

            put("{id}/avbryt", {
                tags = setOf("Gjennomforing")
                operationId = "avbrytGjennomforing"
                request {
                    pathParameterUuid("id")
                    body<AarsakerOgForklaringRequest<AvbrytGjennomforingAarsak>>()
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Gjennomføring ble avbrutt"
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
                val id = call.parameters.getOrFail<UUID>("id")
                val navIdent = getNavIdent()
                val request = call.receive<AarsakerOgForklaringRequest<AvbrytGjennomforingAarsak>>()

                validateAarsakerOgForklaring(request.aarsaker, request.forklaring)
                    .flatMap {
                        gjennomforinger.avbrytGjennomforing(
                            id,
                            tidspunkt = LocalDateTime.now(),
                            aarsakerOgForklaring = request,
                            avbruttAv = navIdent,
                        )
                    }
                    .onLeft {
                        call.respondWithProblemDetail(ValidationError("Klarte ikke avbryte gjennomføring", it))
                    }
                    .onRight {
                        call.respond(HttpStatusCode.OK)
                    }
            }

            put("{id}/tilgjengelig-for-veileder", {
                tags = setOf("Gjennomforing")
                operationId = "setPublisert"
                request {
                    pathParameterUuid("id")
                    body<PublisertRequest>()
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Tilgjengelighet ble oppdatert"
                    }
                    default {
                        description = "Problem details"
                        body<ProblemDetail>()
                    }
                }
            }) {
                val id: UUID by call.parameters
                val navIdent = getNavIdent()
                val request = call.receive<PublisertRequest>()
                gjennomforinger.setPublisert(id, request.publisert, navIdent)
                call.respond(HttpStatusCode.OK)
            }
        }

        authorize(anyOf = setOf(Rolle.TILTAKSGJENNOMFORINGER_SKRIV, Rolle.OPPFOLGER_GJENNOMFORING)) {
            put("{id}/apent-for-pamelding", {
                tags = setOf("Gjennomforing")
                operationId = "setApentForPamelding"
                request {
                    pathParameterUuid("id")
                    body<SetApentForPameldingRequest>()
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Åpent for påmelding ble oppdatert"
                    }
                    default {
                        description = "Problem details"
                        body<ProblemDetail>()
                    }
                }
            }) {
                val id: UUID by call.parameters
                val navIdent = getNavIdent()
                val request = call.receive<SetApentForPameldingRequest>()
                gjennomforinger.setApentForPamelding(id, request.apentForPamelding, navIdent)
                call.respond(HttpStatusCode.OK)
            }

            put("{id}/tilgjengelig-for-arrangor", {
                tags = setOf("Gjennomforing")
                operationId = "setTilgjengeligForArrangor"
                request {
                    pathParameterUuid("id")
                    body<SetTilgjengligForArrangorRequest>()
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Tilgjengelighet ble satt"
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
                val request = call.receive<SetTilgjengligForArrangorRequest>()
                val navIdent = getNavIdent()

                val response = gjennomforinger
                    .setTilgjengeligForArrangorDato(
                        id,
                        request.tilgjengeligForArrangorDato,
                        navIdent,
                    )
                    .mapLeft { ValidationError(errors = it) }

                call.respondWithStatusResponse(response)
            }
        }

        authorize(anyOf = setOf(Rolle.TILTAKSGJENNOMFORINGER_SKRIV)) {
            put("{id}/stengt-hos-arrangor", {
                tags = setOf("Gjennomforing")
                operationId = "setStengtHosArrangor"
                request {
                    pathParameterUuid("id")
                    body<SetStengtHosArrangorRequest>()
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Perioden ble slettet"
                        body<GjennomforingDto>()
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
                val id: UUID by call.pathParameters
                val navIdent = getNavIdent()
                val request = call.receive<SetStengtHosArrangorRequest>()

                val result = request.validate()
                    .flatMap { (periode, beskrivelse) ->
                        gjennomforinger.setStengtHosArrangor(id, periode, beskrivelse, navIdent)
                    }
                    .mapLeft { ValidationError(errors = it) }
                    .map { GjennomforingDtoMapper.fromGjennomforing(it) }

                call.respondWithStatusResponse(result)
            }

            delete("{id}/stengt-hos-arrangor/{periodeId}", {
                tags = setOf("Gjennomforing")
                operationId = "deleteStengtHosArrangor"
                request {
                    pathParameterUuid("id")
                    pathParameter<Int>("periodeId")
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Perioden ble slettet"
                    }
                    default {
                        description = "Problem details"
                        body<ProblemDetail>()
                    }
                }
            }) {
                val id: UUID by call.pathParameters
                val periodeId: Int by call.pathParameters
                val navIdent = getNavIdent()

                gjennomforinger.deleteStengtHosArrangor(id, periodeId, navIdent)

                call.respond(HttpStatusCode.OK)
            }

            delete("{id}/kontaktperson/{kontaktpersonId}", {
                tags = setOf("Gjennomforing")
                operationId = "frikobleGjennomforingKontaktperson"
                request {
                    pathParameterUuid("id")
                    pathParameterUuid("kontaktpersonId")
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Kontaktperson ble frikoblet fra gjennomføringen"
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

                gjennomforinger.frikobleKontaktpersonFraGjennomforing(
                    kontaktpersonId = kontaktpersonId,
                    gjennomforingId = id,
                    navIdent = navIdent,
                )

                call.respond(HttpStatusCode.OK)
            }
        }

        get({
            tags = setOf("Gjennomforing")
            operationId = "getGjennomforinger"
            request {
                queryParameter<String>("search")
                queryParameter<List<String>>("tiltakstyper") {
                    explode = true
                }
                queryParameter<List<GjennomforingStatusType>>("statuser") {
                    explode = true
                }
                queryParameter<List<NavEnhetNummer>>("navEnheter") {
                    explode = true
                }
                queryParameter<List<String>>("arrangorer") {
                    explode = true
                }
                queryParameterUuid("avtaleId")
                queryParameter<Boolean>("publisert")
                queryParameter<Boolean>("visMineGjennomforinger")
                queryParameter<Int>("page")
                queryParameter<Int>("size")
                queryParameter<String>("sort")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Gjennomføringer filtrert på query parameters"
                    body<PaginatedResponse<GjennomforingDto>>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val pagination = getPaginationParams()
            val filter = getAdminTiltaksgjennomforingsFilter()

            val result = gjennomforinger.getAll(pagination, filter)

            call.respond(result)
        }

        get("/excel", {
            tags = setOf("Gjennomforing")
            operationId = "lastNedGjennomforingerSomExcel"
            request {
                queryParameter<String>("search")
                queryParameter<List<String>>("tiltakstyper") {
                    explode = true
                }
                queryParameter<List<GjennomforingStatusType>>("statuser") {
                    explode = true
                }
                queryParameter<List<NavEnhetNummer>>("navEnheter") {
                    explode = true
                }
                queryParameter<List<String>>("arrangorer") {
                    explode = true
                }
                queryParameterUuid("avtaleId")
                queryParameter<Boolean>("publisert")
                queryParameter<Boolean>("visMineGjennomforinger")
                queryParameter<Int>("page")
                queryParameter<Int>("size")
                queryParameter<String>("sort")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Gjennomføringer filtrert på query parameters"
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
            val filter = getAdminTiltaksgjennomforingsFilter()

            val result = gjennomforinger.getAll(pagination, filter)
            val file = ExcelService.createExcelFileForTiltaksgjennomforing(result.data)

            call.response.header(HttpHeaders.AccessControlExposeHeaders, HttpHeaders.ContentDisposition)
            call.response.header(
                HttpHeaders.ContentDisposition,
                ContentDisposition.Attachment
                    .withParameter(ContentDisposition.Parameters.FileName, "gjennomføringer.xlsx")
                    .toString(),
            )
            call.response.header(HttpHeaders.ContentType, ContentType.Application.Xlsx.toString())

            call.respondFile(file)
        }

        get("{id}", {
            tags = setOf("Gjennomforing")
            operationId = "getGjennomforing"
            request {
                pathParameterUuid("id")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Gjennomføringen"
                    body<GjennomforingDto>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val id = call.parameters.getOrFail<UUID>("id")

            gjennomforinger.get(id)
                ?.let { call.respond(GjennomforingDtoMapper.fromGjennomforing(it)) }
                ?: call.respond(HttpStatusCode.NotFound, "Ingen tiltaksgjennomføring med id=$id")
        }

        get("{id}/tiltaksnummer", {
            tags = setOf("Gjennomforing")
            operationId = "getTiltaksnummer"
            request {
                pathParameterUuid("id")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Tiltaksnummer til gjennomføringen"
                    body<TiltaksnummerResponse>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val id: UUID by call.parameters

            gjennomforinger.get(id)
                ?.let { gjennomforing ->
                    gjennomforing.tiltaksnummer
                        ?.let { call.respond(TiltaksnummerResponse(tiltaksnummer = it)) }
                        ?: call.respond(HttpStatusCode.NoContent)
                }
                ?: call.respond(HttpStatusCode.NotFound, "Ingen tiltaksgjennomføring med id=$id")
        }

        get("{id}/historikk", {
            tags = setOf("Gjennomforing")
            operationId = "getGjennomforingEndringshistorikk"
            request {
                pathParameterUuid("id")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Gjennomføringens endringshistorikk"
                    body<EndringshistorikkDto>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val id: UUID by call.parameters
            val historikk = gjennomforinger.getEndringshistorikk(id)
            call.respond(historikk)
        }

        get("{id}/deltaker-summary", {
            tags = setOf("Gjennomforing")
            operationId = "getGjennomforingDeltakerSummary"
            request {
                pathParameterUuid("id")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Oppsummert informasjon om gjennomføringens deltakere"
                    body<GjennomforingDeltakerSummary>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val id: UUID by call.parameters

            val deltakereForGjennomforing = db.session {
                queries.deltaker.getAll(gjennomforingId = id)
            }

            val deltakereByStatus = deltakereForGjennomforing
                .groupBy { it.status.type }
                .map { (status, deltakere) ->
                    DeltakerStatusSummary(status = status.description, count = deltakere.size)
                }

            val summary = GjennomforingDeltakerSummary(
                antallDeltakere = deltakereForGjennomforing.size,
                deltakereByStatus = deltakereByStatus,
            )

            call.respond(summary)
        }

        get("{id}/handlinger", {
            tags = setOf("Gjennomforing")
            operationId = "getGjennomforingHandlinger"
            request {
                pathParameterUuid("id")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Mulige handlinger på gjennomføringer for innlogget bruker"
                    body<Set<GjennomforingHandling>>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val id = call.parameters.getOrFail<UUID>("id")
            val navIdent = getNavIdent()

            gjennomforinger.get(id)
                ?.let { call.respond(gjennomforinger.handlinger(it, navIdent)) }
                ?: call.respond(HttpStatusCode.NotFound, "Det finnes ikke noen avtale med id $id")
        }
    }
}

data class AdminTiltaksgjennomforingFilter(
    val search: String? = null,
    val navEnheter: List<NavEnhetNummer> = emptyList(),
    val tiltakstypeIder: List<UUID> = emptyList(),
    val statuser: List<GjennomforingStatusType> = emptyList(),
    val sortering: String? = null,
    val avtaleId: UUID? = null,
    val arrangorIds: List<UUID> = emptyList(),
    val administratorNavIdent: NavIdent? = null,
    val publisert: Boolean? = null,
    val koordinatorNavIdent: NavIdent? = null,
)

fun RoutingContext.getAdminTiltaksgjennomforingsFilter(): AdminTiltaksgjennomforingFilter {
    val search = call.request.queryParameters["search"]
    val navEnheter = call.parameters.getAll("navEnheter")?.map { NavEnhetNummer(it) } ?: emptyList()
    val tiltakstypeIder = call.parameters.getAll("tiltakstyper")?.map { UUID.fromString(it) } ?: emptyList()
    val statuser = call.parameters.getAll("statuser")
        ?.map { GjennomforingStatusType.valueOf(it) }
        ?: emptyList()
    val sortering = call.request.queryParameters["sort"]
    val avtaleId = call.request.queryParameters["avtaleId"]?.let { if (it.isEmpty()) null else UUID.fromString(it) }
    val arrangorIds = call.parameters.getAll("arrangorer")?.map { UUID.fromString(it) } ?: emptyList()
    val publisert = call.request.queryParameters["publisert"]?.toBoolean()
    val administratorNavIdent = call.parameters["visMineGjennomforinger"]
        ?.takeIf { it == "true" }
        ?.let { getNavIdent() }

    return AdminTiltaksgjennomforingFilter(
        search = search,
        navEnheter = navEnheter,
        tiltakstypeIder = tiltakstypeIder,
        statuser = statuser,
        sortering = sortering,
        avtaleId = avtaleId,
        arrangorIds = arrangorIds,
        publisert = publisert,
        administratorNavIdent = administratorNavIdent,
        koordinatorNavIdent = administratorNavIdent,
    )
}

@Serializable
data class GjennomforingDeltakerSummary(
    val antallDeltakere: Int,
    val deltakereByStatus: List<DeltakerStatusSummary>,
)

@Serializable
data class DeltakerStatusSummary(
    val status: String,
    val count: Int,
)

@Serializable
data class TiltaksnummerResponse(
    val tiltaksnummer: String,
)

@Serializable
data class GjennomforingRequest(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = UUIDSerializer::class)
    val tiltakstypeId: UUID,
    @Serializable(with = UUIDSerializer::class)
    val avtaleId: UUID,
    val navn: String,
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val sluttDato: LocalDate?,
    val antallPlasser: Int,
    @Serializable(with = UUIDSerializer::class)
    val arrangorId: UUID,
    val arrangorKontaktpersoner: List<
        @Serializable(with = UUIDSerializer::class)
        UUID,
        >,
    val veilederinformasjon: VeilederinfoRequest,
    val kontaktpersoner: List<GjennomforingKontaktpersonDto>,
    val administratorer: List<NavIdent>,
    val oppstart: GjennomforingOppstartstype,
    val stedForGjennomforing: String?,
    val deltidsprosent: Double,
    val estimertVentetid: EstimertVentetid?,
    @Serializable(with = LocalDateSerializer::class)
    val tilgjengeligForArrangorDato: LocalDate?,
    val amoKategorisering: AmoKategorisering?,
    val utdanningslop: UtdanningslopDbo? = null,
)

@Serializable
data class GjennomforingKontaktpersonDto(
    val navIdent: NavIdent,
    val beskrivelse: String?,
)

@Serializable
data class PublisertRequest(
    val publisert: Boolean,
)

@Serializable
data class SetApentForPameldingRequest(
    val apentForPamelding: Boolean,
)

@Serializable
data class SetStengtHosArrangorRequest(
    @Serializable(with = LocalDateSerializer::class)
    val periodeStart: LocalDate? = null,
    @Serializable(with = LocalDateSerializer::class)
    val periodeSlutt: LocalDate? = null,
    val beskrivelse: String? = null,
) {
    fun validate(): Either<NonEmptyList<FieldError>, Pair<Periode, String>> = either {
        zipOrAccumulate(
            {
                ensure(!beskrivelse.isNullOrBlank()) {
                    FieldError.of(
                        detail = "Du må legge inn en beskrivelse",
                        SetStengtHosArrangorRequest::beskrivelse,
                    )
                }
                beskrivelse
            },
            {
                ensureNotNull(periodeStart) {
                    FieldError.of(
                        detail = "Du må legge inn start på perioden",
                        SetStengtHosArrangorRequest::periodeStart,
                    )
                }
            },
            {
                ensureNotNull(periodeSlutt) {
                    FieldError.of(
                        detail = "Du må legge inn slutt på perioden",
                        SetStengtHosArrangorRequest::periodeSlutt,
                    )
                }
            },
        ) { beskrivelse, start, slutt ->
            ensure(!slutt.isBefore(start)) {
                FieldError.of(
                    detail = "Start må være før slutt",
                    SetStengtHosArrangorRequest::periodeStart,
                ).nel()
            }

            val periode = Periode.fromInclusiveDates(start, slutt)

            Pair(periode, beskrivelse)
        }
    }
}

@Serializable
data class SetTilgjengligForArrangorRequest(
    @Serializable(with = LocalDateSerializer::class)
    val tilgjengeligForArrangorDato: LocalDate?,
)

@Serializable
data class EstimertVentetid(
    val verdi: Int,
    val enhet: String,
)

@Serializable
enum class GjennomforingHandling {
    PUBLISER,
    REDIGER,
    AVBRYT,
    DUPLISER,
    ENDRE_APEN_FOR_PAMELDING,
    ENDRE_TILGJENGELIG_FOR_ARRANGOR,
    REGISTRER_STENGT_HOS_ARRANGOR,
    OPPRETT_TILSAGN,
    OPPRETT_EKSTRATILSAGN,
    OPPRETT_TILSAGN_FOR_INVESTERINGER,
    OPPRETT_KORREKSJON_PA_UTBETALING,
}
