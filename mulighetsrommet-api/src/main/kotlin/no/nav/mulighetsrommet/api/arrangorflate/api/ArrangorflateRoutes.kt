package no.nav.mulighetsrommet.api.arrangorflate.api

import arrow.core.flatMap
import arrow.core.getOrElse
import io.github.smiley4.ktoropenapi.config.RequestConfig
import io.github.smiley4.ktoropenapi.config.RequestParameterConfig
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import io.ktor.utils.io.*
import io.swagger.v3.oas.models.media.Schema
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import no.nav.mulighetsrommet.api.arrangor.ArrangorService
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorDto
import no.nav.mulighetsrommet.api.arrangorflate.ArrangorFlateService
import no.nav.mulighetsrommet.api.avtale.model.Prismodell
import no.nav.mulighetsrommet.api.clients.kontoregisterOrganisasjon.KontonummerRegisterOrganisasjonError
import no.nav.mulighetsrommet.api.pdfgen.PdfGenClient
import no.nav.mulighetsrommet.api.plugins.ArrangorflatePrincipal
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.api.utbetaling.UtbetalingService
import no.nav.mulighetsrommet.api.utbetaling.UtbetalingValidator
import no.nav.mulighetsrommet.api.utbetaling.mapper.UbetalingToPdfDocumentContentMapper
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.brreg.BrregError
import no.nav.mulighetsrommet.clamav.ClamAvClient
import no.nav.mulighetsrommet.clamav.Content
import no.nav.mulighetsrommet.clamav.Status
import no.nav.mulighetsrommet.clamav.Vedlegg
import no.nav.mulighetsrommet.ktor.exception.BadRequest
import no.nav.mulighetsrommet.ktor.exception.InternalServerError
import no.nav.mulighetsrommet.ktor.exception.NotFound
import no.nav.mulighetsrommet.ktor.exception.StatusException
import no.nav.mulighetsrommet.ktor.plugins.respondWithProblemDetail
import no.nav.mulighetsrommet.model.Arrangor
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.ProblemDetail
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import no.nav.tiltak.okonomi.Tilskuddstype
import org.koin.ktor.ext.inject
import java.util.*

fun Route.arrangorflateRoutes() {
    val arrangorService: ArrangorService by inject()
    val utbetalingService: UtbetalingService by inject()
    val pdfClient: PdfGenClient by inject()
    val arrangorFlateService: ArrangorFlateService by inject()
    val clamAvClient: ClamAvClient by inject()

    fun RoutingContext.arrangorTilganger(): List<Organisasjonsnummer>? {
        return call.principal<ArrangorflatePrincipal>()?.organisasjonsnummer
    }

    fun RoutingContext.requireTilgangHosArrangor(organisasjonsnummer: Organisasjonsnummer) = arrangorTilganger()
        ?.find { it == organisasjonsnummer }
        ?: throw StatusException(HttpStatusCode.Forbidden, "Ikke tilgang til bedrift")

    suspend fun resolveArrangor(organisasjonsnummer: Organisasjonsnummer): ArrangorDto {
        return arrangorService.getArrangorOrSyncFromBrreg(organisasjonsnummer)
            .getOrElse {
                when (it) {
                    is BrregError.NotFound -> throw StatusException(
                        HttpStatusCode.BadRequest,
                        "Fant ikke arrangør $organisasjonsnummer i Brreg",
                    )

                    is BrregError.FjernetAvJuridiskeArsaker -> throw StatusException(
                        HttpStatusCode.Forbidden,
                        "Arrangør $organisasjonsnummer er fjernet av juridiske årsaker",
                    )

                    is BrregError.BadRequest, BrregError.Error -> throw StatusException(
                        HttpStatusCode.InternalServerError,
                        "Feil oppsto ved henting av arrangør $organisasjonsnummer fra Brreg",
                    )
                }
            }
    }

    fun RoutingContext.getTilsagnOrRespondNotFound(): ArrangorflateTilsagnDto {
        val id: UUID by call.parameters
        return arrangorFlateService.getTilsagn(id) ?: throw NotFoundException("Fant ikke tilsagn med id=$id")
    }

    fun RoutingContext.getUtbetalingOrRespondNotFound(): Utbetaling {
        val id: UUID by call.parameters
        return arrangorFlateService.getUtbetaling(id) ?: throw NotFoundException("Fant ikke utbetaling med id=$id")
    }

    get("/tilgang-arrangor", {
        description = "Henter liste over arrangører innlogget bruker har tilgang til"
        tags = setOf("Arrangorflate")
        operationId = "getArrangorerInnloggetBrukerHarTilgangTil"
        response {
            code(HttpStatusCode.OK) {
                description = "Arrangører"
                body<List<ArrangorDto>>()
            }
            default {
                description = "Problem details"
                body<ProblemDetail>()
            }
        }
    }) {
        val arrangorer = arrangorTilganger()
            ?.map { resolveArrangor(it) }
            ?: throw StatusException(HttpStatusCode.Unauthorized, "Mangler altinn tilgang")

        call.respond(arrangorer)
    }

    route("/arrangor/{orgnr}") {
        get("/kontonummer", {
            description = "Hent kontonummer fra kontonummer-organisasjon for gitt organisasjonsnummer"
            tags = setOf("Arrangorflate")
            operationId = "getKontonummer"
            request {
                pathParameter<Organisasjonsnummer>("orgnr")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Kontonummer til arrangør"
                    body<KontonummerResponse>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val orgnr = call.parameters.getOrFail("orgnr").let { Organisasjonsnummer(it) }

            requireTilgangHosArrangor(orgnr)

            arrangorFlateService.getKontonummer(orgnr)
                .onLeft {
                    val error = when (it) {
                        KontonummerRegisterOrganisasjonError.UgyldigInput -> BadRequest("Ugyldig input")
                        KontonummerRegisterOrganisasjonError.FantIkkeKontonummer -> NotFound("Organisasjon mangler kontonummer")
                        KontonummerRegisterOrganisasjonError.Error -> InternalServerError("Klarte ikke hente kontonummer for organisasjon")
                    }
                    call.respondWithProblemDetail(error)
                }
                .onRight { kontonummer ->
                    call.respond(KontonummerResponse(kontonummer))
                }
        }

        get("/gjennomforing", {
            description = "Hent gjennomføringene til arrangør"
            tags = setOf("Arrangorflate")
            operationId = "getArrangorflateGjennomforinger"
            request {
                pathParameter<Organisasjonsnummer>("orgnr")
                queryParameter<List<Prismodell>>("prismodeller") {
                    explode = true
                }
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Arrangør sine gjennomføringer"
                    body<List<ArrangorflateGjennomforing>>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val orgnr = call.parameters.getOrFail("orgnr").let { Organisasjonsnummer(it) }
            requireTilgangHosArrangor(orgnr)

            val prismodeller = call.parameters.getAll("prismodeller")
                ?.map { Prismodell.valueOf(it) }
                ?: emptyList()

            call.respond(arrangorFlateService.getGjennomforinger(orgnr, prismodeller))
        }

        get("/utbetaling", {
            description = "Hent utbetalingene til arrangør"
            tags = setOf("Arrangorflate")
            operationId = "getAllUtbetaling"
            request {
                pathParameter<Organisasjonsnummer>("orgnr")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Arrangør sine utbetalinger"
                    body<ArrFlateUtbetalinger>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val orgnr = call.parameters.getOrFail("orgnr").let { Organisasjonsnummer(it) }

            requireTilgangHosArrangor(orgnr)

            val utbetalinger = arrangorFlateService.getUtbetalinger(orgnr)

            call.respond(utbetalinger)
        }

        post("/utbetaling", {
            description = "Opprett krav om utbetaling"
            tags = setOf("Arrangorflate")
            operationId = "opprettKravOmUtbetaling"
            request {
                pathParameter<Organisasjonsnummer>("orgnr")
                body<OpprettKravOmUtbetalingRequest> {
                    description = "Request for creating a payment claim"
                    mediaTypes(ContentType.MultiPart.FormData)
                }
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Informasjon om opprettet krav om utbetaling"
                    body<OpprettKravOmUtbetalingResponse>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val orgnr = call.parameters.getOrFail("orgnr").let { Organisasjonsnummer(it) }

            requireTilgangHosArrangor(orgnr)
            val request = receiveOpprettKravOmUtbetalingRequest(call)

            // Scan vedlegg for virus
            if (clamAvClient.virusScanVedlegg(request.vedlegg).any { it.Result == Status.FOUND }) {
                return@post call.respondWithProblemDetail(BadRequest("Virus funnet i minst ett vedlegg"))
            }

            UtbetalingValidator.validateOpprettKravOmUtbetaling(request)
                .flatMap { utbetalingService.opprettUtbetaling(it, Arrangor) }
                .onLeft { errors ->
                    call.respondWithProblemDetail(ValidationError("Klarte ikke opprette utbetaling", errors))
                }
                .onRight { utbetaling ->
                    call.respondText(utbetaling.id.toString())
                }
        }

        route("/tilsagn") {
            get({
                description = "Hent alle tilsagn til arrangør"
                tags = setOf("Arrangorflate")
                operationId = "getAllArrangorflateTilsagn"
                request {
                    pathParameter<Organisasjonsnummer>("orgnr")
                    queryParameter<List<TilsagnStatus>>("statuser") {
                        explode = true
                    }
                    queryParameter<List<TilsagnType>>("typer") {
                        explode = true
                    }
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Alle tilsagn for gitt organisasjonsnummer"
                        body<List<ArrangorflateTilsagnDto>>()
                    }
                    default {
                        description = "Problem details"
                        body<ProblemDetail>()
                    }
                }
            }) {
                val orgnr = call.parameters.getOrFail("orgnr").let { Organisasjonsnummer(it) }

                requireTilgangHosArrangor(orgnr)

                val filter = getArrFlateTilsagnFilter()
                val tilsagn = arrangorFlateService.getTilsagn(filter, orgnr)

                call.respond(tilsagn)
            }
        }
    }

    route("/tilsagn/{id}") {
        get({
            description = "Hent tilsagn"
            tags = setOf("Arrangorflate")
            operationId = "getArrangorflateTilsagn"
            request {
                pathParameterUuid("id")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Tilsagn for gitt id"
                    body<ArrangorflateTilsagnDto>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val tilsagn = getTilsagnOrRespondNotFound()

            requireTilgangHosArrangor(tilsagn.arrangor.organisasjonsnummer)

            call.respond(tilsagn)
        }
    }

    route("/utbetaling/{id}") {
        get({
            description = "Hent utbetaling for arrangør"
            tags = setOf("Arrangorflate")
            operationId = "getArrFlateUtbetaling"
            request {
                pathParameterUuid("id")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Utbetaling for gitt id"
                    body<ArrFlateUtbetaling>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val utbetaling = getUtbetalingOrRespondNotFound()

            requireTilgangHosArrangor(utbetaling.arrangor.organisasjonsnummer)

            call.respond(arrangorFlateService.toArrFlateUtbetaling(utbetaling))
        }

        get("/advarsler", {
            description = "Hent deltakerforslag som kan påvirke utbetalingen"
            tags = setOf("Arrangorflate")
            operationId = "getRelevanteForslag"
            request {
                pathParameterUuid("id")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Deltakerforslag som er relevante for utbetalingen"
                    body<List<DeltakerAdvarsel>>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val utbetaling = getUtbetalingOrRespondNotFound()

            requireTilgangHosArrangor(utbetaling.arrangor.organisasjonsnummer)

            call.respond(arrangorFlateService.getAdvarsler(utbetaling))
        }

        post("/godkjenn", {
            description = "Godkjenn en utbetaling på vegne av arrangør"
            tags = setOf("Arrangorflate")
            operationId = "godkjennUtbetaling"
            request {
                pathParameterUuid("id")
                body<GodkjennUtbetaling>()
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Utbetaling ble godkjent"
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
            val utbetaling = getUtbetalingOrRespondNotFound()

            requireTilgangHosArrangor(utbetaling.arrangor.organisasjonsnummer)

            val request = call.receive<GodkjennUtbetaling>()

            val advarsler = arrangorFlateService.getAdvarsler(utbetaling)

            UtbetalingValidator
                .validerGodkjennUtbetaling(
                    request,
                    utbetaling,
                    advarsler,
                )
                .onLeft {
                    call.respondWithProblemDetail(ValidationError(errors = it))
                }
                .onRight {
                    utbetalingService.godkjentAvArrangor(utbetaling.id, it)
                    call.respond(HttpStatusCode.OK)
                }
        }

        get("/pdf", {
            description = "Hent pdf med status på utbetalingen"
            tags = setOf("Arrangorflate")
            operationId = "getUtbetalingPdf"
            request {
                pathParameterUuid("id")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Informasjon om utbetalingen"
                    body<String> {
                        mediaTypes(ContentType.Application.Pdf)
                    }
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val utbetaling = getUtbetalingOrRespondNotFound()

            requireTilgangHosArrangor(utbetaling.arrangor.organisasjonsnummer)

            val arrflateUtbetaling = arrangorFlateService.toArrFlateUtbetaling(utbetaling)
            val content = UbetalingToPdfDocumentContentMapper.toUtbetalingsdetaljerPdfContent(arrflateUtbetaling)
            pdfClient.getPdfDocument(content)
                .onRight { pdfContent ->
                    call.response.headers.append(
                        "Content-Disposition",
                        "attachment; filename=\"utbetaling.pdf\"",
                    )
                    call.respondBytes(pdfContent, contentType = ContentType.Application.Pdf)
                }
                .onLeft { error ->
                    application.log.warn("Klarte ikke lage PDF. Response fra pdfgen: $error")
                    call.respondWithProblemDetail(InternalServerError("Klarte ikke lage PDF"))
                }
        }

        get("/tilsagn", {
            description = "Hent alle tilsagn som kan benyttes til utbetalingen"
            tags = setOf("Arrangorflate")
            operationId = "getArrangorflateTilsagnTilUtbetaling"
            request {
                pathParameterUuid("id")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Alle tilsagn som kan benyttes til utbetalingen"
                    body<List<ArrangorflateTilsagnDto>>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val utbetaling = getUtbetalingOrRespondNotFound()

            requireTilgangHosArrangor(utbetaling.arrangor.organisasjonsnummer)

            val tilsagn = arrangorFlateService.getArrangorflateTilsagnTilUtbetaling(utbetaling)

            call.respond(tilsagn)
        }

        get("/sync-kontonummer", {
            description =
                "Oppdaterer utbetalingen med nyeste kontonummer registrert for arrangør sitt organisasjonsnummer"
            tags = setOf("Arrangorflate")
            operationId = "synkroniserKontonummerForUtbetaling"
            request {
                pathParameterUuid("id")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Kontonummer som blir benyttet for utbetalingen"
                    body<KontonummerResponse>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val utbetaling = getUtbetalingOrRespondNotFound()

            requireTilgangHosArrangor(utbetaling.arrangor.organisasjonsnummer)

            arrangorFlateService.synkroniserKontonummer(utbetaling)
                .onLeft { error ->
                    val error = when (error) {
                        KontonummerRegisterOrganisasjonError.UgyldigInput -> BadRequest("Ugyldig input")
                        KontonummerRegisterOrganisasjonError.FantIkkeKontonummer -> NotFound("Organisasjon mangler kontonummer")
                        KontonummerRegisterOrganisasjonError.Error -> InternalServerError("Klarte ikke hente kontonummer for organisasjon")
                    }
                    call.respondWithProblemDetail(error)
                }
                .onRight { kontonummer ->
                    call.respond(KontonummerResponse(kontonummer))
                }
        }
    }
}

private fun RequestConfig.pathParameterUuid(name: String, block: RequestParameterConfig.() -> Unit = {}) {
    pathParameter(
        name,
        Schema<Any>().also {
            it.types = setOf("string")
            it.format = "uuid"
        },
        block,
    )
}

private suspend fun receiveOpprettKravOmUtbetalingRequest(call: RoutingCall): OpprettKravOmUtbetalingRequest {
    var gjennomforingId: UUID? = null
    var tilsagnId: UUID? = null
    var periodeStart: String? = null
    var periodeSlutt: String? = null
    var kontonummer: String? = null
    var kidNummer: String? = null
    var belop: Int? = null
    var tilskuddstype: Tilskuddstype? = null
    val vedlegg: MutableList<Vedlegg> = mutableListOf()
    val multipart = call.receiveMultipart(formFieldLimit = 1024 * 1024 * 100)

    multipart.forEachPart { part ->
        when (part) {
            is PartData.FormItem -> {
                when (part.name) {
                    "gjennomforingId" -> gjennomforingId = UUID.fromString(part.value)
                    "tilsagnId" -> tilsagnId = UUID.fromString(part.value)
                    "kontonummer" -> kontonummer = part.value
                    "kidNummer" -> kidNummer = part.value
                    "belop" -> belop = part.value.toInt()
                    "periodeStart" -> periodeStart = part.value
                    "periodeSlutt" -> periodeSlutt = part.value
                    "tilskuddstype" -> tilskuddstype = Tilskuddstype.valueOf(part.value)
                }
            }

            is PartData.FileItem -> {
                if (part.name == "vedlegg") {
                    vedlegg.add(
                        Vedlegg(
                            content = Content(
                                contentType = part.contentType.toString(),
                                content = part.provider().toByteArray(),
                            ),
                            filename = part.originalFileName ?: "ukjent.pdf",
                        ),
                    )
                }
            }

            else -> {}
        }

        part.dispose()
    }

    val validatedVedlegg = vedlegg.map { v ->
        // Optionally validate file type and size here
        val fileName = v.filename
        val contentType = v.content.contentType

        require(contentType.equals("application/pdf", ignoreCase = true)) {
            "Vedlegg $fileName er ikke en PDF"
        }

        v
    }

    return OpprettKravOmUtbetalingRequest(
        gjennomforingId = requireNotNull(gjennomforingId) { "Mangler gjennomforingId" },
        tilsagnId = requireNotNull(tilsagnId) { "Mangler tilsagnId" },
        periodeStart = requireNotNull(periodeStart) { "Mangler periodeStart" },
        periodeSlutt = requireNotNull(periodeSlutt) { "Mangler periodeSlutt" },
        kontonummer = requireNotNull(kontonummer) { "Mangler kontonummer" },
        kidNummer = kidNummer,
        belop = belop ?: 0,
        tilskuddstype = requireNotNull(tilskuddstype) { "Mangler tilskuddstype" },
        vedlegg = validatedVedlegg,
    )
}

@Serializable
data class KontonummerResponse(
    val kontonummer: Kontonummer,
)

@Serializable
data class GodkjennUtbetaling(
    val digest: String,
    val kid: String?,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed class DeltakerAdvarsel {
    abstract val deltakerId: UUID

    @Serializable
    @SerialName("DeltakerAdvarselRelevanteForslag")
    data class RelevanteForslag(
        @Serializable(with = UUIDSerializer::class)
        override val deltakerId: UUID,
        val antallRelevanteForslag: Int,
    ) : DeltakerAdvarsel()

    @Serializable
    @SerialName("DeltakerAdvarselFeilSluttDato")
    data class FeilSluttDato(
        @Serializable(with = UUIDSerializer::class)
        override val deltakerId: UUID,
    ) : DeltakerAdvarsel()

    @Serializable
    @SerialName("DeltakerAdvarselOverlappendePeriode")
    data class OverlappendePeriode(
        @Serializable(with = UUIDSerializer::class)
        override val deltakerId: UUID,
    ) : DeltakerAdvarsel()
}

@Serializable
data class OpprettKravOmUtbetalingRequest(
    @Serializable(with = UUIDSerializer::class)
    val gjennomforingId: UUID,
    @Serializable(with = UUIDSerializer::class)
    val tilsagnId: UUID,
    val periodeStart: String,
    val periodeSlutt: String,
    val kontonummer: String,
    val kidNummer: String? = null,
    val belop: Int,
    val vedlegg: List<Vedlegg>,
    val tilskuddstype: Tilskuddstype,
)

data class ArrFlateTilsagnFilter(
    val statuser: List<TilsagnStatus>? = null,
    val typer: List<TilsagnType>? = null,
)

fun RoutingContext.getArrFlateTilsagnFilter(): ArrFlateTilsagnFilter {
    return ArrFlateTilsagnFilter(
        statuser = call.parameters.getAll("statuser")?.map { TilsagnStatus.valueOf(it) },
        typer = call.parameters.getAll("typer")?.map { TilsagnType.valueOf(it) },
    )
}

@Serializable
data class ArrFlateUtbetalinger(
    val aktive: List<ArrFlateUtbetalingKompaktDto>,
    val historiske: List<ArrFlateUtbetalingKompaktDto>,
)

@Serializable
data class OpprettKravOmUtbetalingResponse(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
)
