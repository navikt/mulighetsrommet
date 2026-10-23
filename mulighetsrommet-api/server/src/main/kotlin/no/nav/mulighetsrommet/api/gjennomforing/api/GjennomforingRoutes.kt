package no.nav.mulighetsrommet.api.gjennomforing.api

import arrow.core.Either
import arrow.core.flatMap
import io.github.smiley4.ktoropenapi.delete
import io.github.smiley4.ktoropenapi.get
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
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.route
import io.ktor.server.util.getOrFail
import io.ktor.server.util.getValue
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.admin.aarsakerbegrunnelse.AarsakerOgBegrunnelseRequest
import no.nav.mulighetsrommet.admin.gjennomforing.AdminTiltaksgjennomforingFilter
import no.nav.mulighetsrommet.admin.gjennomforing.GjennomforingAvtaleService
import no.nav.mulighetsrommet.admin.gjennomforing.GjennomforingDetaljerDto
import no.nav.mulighetsrommet.admin.gjennomforing.GjennomforingDetaljerError
import no.nav.mulighetsrommet.admin.gjennomforing.GjennomforingDetaljerRequest
import no.nav.mulighetsrommet.admin.gjennomforing.GjennomforingDetaljerService
import no.nav.mulighetsrommet.admin.gjennomforing.GjennomforingHandling
import no.nav.mulighetsrommet.admin.gjennomforing.GjennomforingKompaktDto
import no.nav.mulighetsrommet.admin.gjennomforing.GjennomforingRequest
import no.nav.mulighetsrommet.admin.gjennomforing.GjennomforingVeilederinfoRequest
import no.nav.mulighetsrommet.admin.gjennomforing.SetStengtHosArrangorRequest
import no.nav.mulighetsrommet.admin.gjennomforing.SetTilgjengligForArrangorRequest
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.domain.gjennomforing.AvbrytGjennomforingAarsak
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingAvtaleDetaljer
import no.nav.mulighetsrommet.api.domain.navansatt.Rolle
import no.nav.mulighetsrommet.api.navansatt.ktor.authorize
import no.nav.mulighetsrommet.api.parameters.getPaginationParams
import no.nav.mulighetsrommet.api.plugins.getAccessType
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.plugins.pathParameterUuid
import no.nav.mulighetsrommet.api.responses.PaginatedResponse
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.responses.respondWithStatusResponse
import no.nav.mulighetsrommet.api.utbetaling.service.ManglerTilgangTilPerson
import no.nav.mulighetsrommet.api.utils.DatoUtils.parseOrNull
import no.nav.mulighetsrommet.ktor.exception.BadRequest
import no.nav.mulighetsrommet.ktor.exception.Forbidden
import no.nav.mulighetsrommet.ktor.exception.NotFound
import no.nav.mulighetsrommet.ktor.plugins.respondWithProblemDetail
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.ProblemDetail
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import no.nav.mulighetsrommet.tokenprovider.requireAzureAd
import no.nav.mulighetsrommet.validation.FieldError
import no.nav.mulighetsrommet.validation.validation
import org.koin.ktor.ext.inject
import java.time.LocalDate
import java.util.UUID

fun Route.gjennomforingRoutes() {
    val db: ApiDatabase by inject()
    val avtaleGjennomforinger: GjennomforingAvtaleService by inject()
    val gjennomforinger: GjennomforingDetaljerService by inject()

    route("gjennomforinger") {
        authorize(Rolle.TILTAKSGJENNOMFORINGER_SKRIV) {
            put({
                tags = setOf("Gjennomforing")
                operationId = "createGjennomforing"
                request {
                    body<GjennomforingRequest>()
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Gjennomføring ble opprettet"
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

                val result = avtaleGjennomforinger.create(request, navIdent)
                    .mapLeft { ValidationError(errors = it) }
                    .map { HttpStatusCode.OK }

                call.respondWithStatusResponse(result)
            }

            put("{id}/detaljer", {
                tags = setOf("Gjennomforing")
                operationId = "updateGjennomforingDetaljer"
                request {
                    pathParameterUuid("id")
                    body<GjennomforingDetaljerRequest>()
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Detaljer ble oppdatert"
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
                val request = call.receive<GjennomforingDetaljerRequest>()

                val result = avtaleGjennomforinger.updateDetaljer(id, request, navIdent)
                    .mapLeft { ValidationError(errors = it) }
                    .map { HttpStatusCode.OK }

                call.respondWithStatusResponse(result)
            }

            put("{id}/veilederinformasjon", {
                tags = setOf("Gjennomforing")
                operationId = "updateGjennomforingVeilederinformasjon"
                request {
                    pathParameterUuid("id")
                    body<GjennomforingVeilederinfoRequest>()
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Veilederinformasjon ble oppdatert"
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
                val request = call.receive<GjennomforingVeilederinfoRequest>()

                val result = avtaleGjennomforinger.updateVeilederinfo(id, request, navIdent)
                    .mapLeft { ValidationError(errors = it) }
                    .map { HttpStatusCode.OK }

                call.respondWithStatusResponse(result)
            }

            put("{id}/avbryt", {
                tags = setOf("Gjennomforing")
                operationId = "avbrytGjennomforing"
                request {
                    pathParameterUuid("id")
                    body<AarsakerOgBegrunnelseRequest<AvbrytGjennomforingAarsak>>()
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
                val request = call.receive<AarsakerOgBegrunnelseRequest<AvbrytGjennomforingAarsak>>()

                request.validate()
                    .flatMap {
                        avtaleGjennomforinger.avbrytGjennomforing(
                            id,
                            sluttDato = LocalDate.now(),
                            aarsakerOgBegrunnelse = it,
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

            put("{id}/gjenapne", {
                tags = setOf("Gjennomforing")
                operationId = "gjenapneGjennomforing"
                request {
                    pathParameterUuid("id")
                    body<GjenapneGjennomforingRequest>()
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Gjennomføring ble gjenåpnet"
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
                val request = call.receive<GjenapneGjennomforingRequest>()

                avtaleGjennomforinger.gjenapneGjennomforing(id, request.nySluttDato, navIdent)
                    .onLeft {
                        call.respondWithProblemDetail(ValidationError("Klarte ikke gjenåpne gjennomføring", it))
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
                avtaleGjennomforinger.setPublisert(id, request.publisert, navIdent)
                call.respond(HttpStatusCode.OK)
            }
        }

        authorize(anyOf = setOf(Rolle.TILTAKSGJENNOMFORINGER_SKRIV, Rolle.OPPFOLGER_GJENNOMFORING)) {
            put("{id}/estimert-ventetid", {
                tags = setOf("Gjennomforing")
                operationId = "setEstimertVentetid"
                request {
                    pathParameterUuid("id")
                    body<SetEstimertVentetidRequest>()
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Estimert ventetid ble oppdatert"
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
                val request = call.receive<SetEstimertVentetidRequest>()

                request.validate()
                    .onLeft {
                        call.respondWithProblemDetail(ValidationError("Klarte ikke sette estimert ventetid", it))
                    }
                    .onRight { estimertVentetid ->
                        avtaleGjennomforinger.setEstimertVentetid(id, estimertVentetid, navIdent)
                        call.respond(HttpStatusCode.OK)
                    }
            }

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
                avtaleGjennomforinger.setApentForPamelding(id, request.apentForPamelding, navIdent)
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

                val response = avtaleGjennomforinger
                    .setTilgjengeligForArrangorDato(
                        id,
                        request.tilgjengeligForArrangorDato?.parseOrNull(),
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
                        avtaleGjennomforinger.setStengtHosArrangor(id, periode, beskrivelse, navIdent)
                    }
                    .mapLeft { ValidationError(errors = it) }

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

                avtaleGjennomforinger.deleteStengtHosArrangor(id, periodeId, navIdent)

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

                avtaleGjennomforinger.frikobleKontaktpersonFraGjennomforing(
                    kontaktpersonId = kontaktpersonId,
                    gjennomforingId = id,
                    navIdent = navIdent,
                )

                call.respond(HttpStatusCode.OK)
            }
        }

        post({
            tags = setOf("Gjennomforing")
            operationId = "getGjennomforinger"
            request {
                queryParameter<Int>("page")
                queryParameter<Int>("size")
                body<GetGjennomforingerRequest>()
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Gjennomføringer"
                    body<PaginatedResponse<GjennomforingKompaktDto>>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val pagination = getPaginationParams()
            val filter = getAdminTiltaksgjennomforingFilter()
            val accessType = call.getAccessType().requireAzureAd()

            gjennomforinger.getAllKompaktDto(pagination, filter, accessType)
                .onLeft {
                    call.respondWithProblemDetail(ManglerTilgangTilPerson.fromAvvistGrunn(it))
                }
                .onRight {
                    call.respond(PaginatedResponse.of(pagination, it.totalCount, it.items))
                }
        }

        post("/excel", {
            tags = setOf("Gjennomforing")
            operationId = "lastNedGjennomforingerSomExcel"
            request {
                body<GetGjennomforingerRequest>()
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Gjennomføringer eksportert til Excel"
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
            val filter = getAdminTiltaksgjennomforingFilter()
            val accessType = call.getAccessType().requireAzureAd()

            gjennomforinger.exportToExcel(filter, accessType)
                .onLeft {
                    call.respondWithProblemDetail(ManglerTilgangTilPerson.fromAvvistGrunn(it))
                }
                .onRight {
                    call.response.header(HttpHeaders.AccessControlExposeHeaders, HttpHeaders.ContentDisposition)
                    call.response.header(
                        HttpHeaders.ContentDisposition,
                        ContentDisposition.Attachment
                            .withParameter(ContentDisposition.Parameters.FileName, "gjennomføringer.xlsx")
                            .toString(),
                    )
                    call.response.header(HttpHeaders.ContentType, ContentType.Application.Xlsx.toString())

                    call.respondFile(it)
                }
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
                    body<GjennomforingDetaljerDto>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val id = call.parameters.getOrFail<UUID>("id")
            val accessType = call.getAccessType().requireAzureAd()
            val navIdent = getNavIdent()

            val result = gjennomforinger.getGjennomforingDetaljerDto(id, accessType, navIdent)
                .mapLeft {
                    toProblemDetail(it)
                }
            call.respondWithStatusResponse(result)
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

            val gjennomforing = gjennomforinger.getGjennomforingTiltaksadministrasjon(id)
                ?: return@get call.respondUkjentGjennomforing(id)

            gjennomforing.arena?.tiltaksnummer
                ?.let { call.respond(TiltaksnummerResponse(tiltaksnummer = it.value)) }
                ?: call.respond(HttpStatusCode.NoContent)
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
                repository.deltaker.getByGjennomforing(id)
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

            val handlinger = gjennomforinger.getHandlinger(id, navIdent)

            call.respond(handlinger)
        }
    }
}

private suspend fun RoutingCall.respondUkjentGjennomforing(id: UUID) {
    respondWithProblemDetail(BadRequest("Ingen tiltaksgjennomføring med id=$id"))
}

fun toProblemDetail(error: GjennomforingDetaljerError): ProblemDetail = when (error) {
    is GjennomforingDetaljerError.IkkeFunnet -> NotFound(error.message)
    is GjennomforingDetaljerError.ManglerTilgang -> Forbidden(error.message)
    is GjennomforingDetaljerError.PersonaliaSjekkAvvist -> ManglerTilgangTilPerson.fromAvvistGrunn(error.avvistGrunn)
}

@Serializable
data class GetGjennomforingerRequest(
    val search: String? = null,
    val navEnheter: List<NavEnhetNummer> = emptyList(),
    val tiltakstyper: List<Tiltakskode> = emptyList(),
    val statuser: List<GjennomforingStatusType> = emptyList(),
    val sort: String? = null,
    @Serializable(with = UUIDSerializer::class)
    val avtaleId: UUID? = null,
    val arrangorer: List<
        @Serializable(with = UUIDSerializer::class)
        UUID,
        > = emptyList(),
    val publisert: Boolean? = null,
    val visMineGjennomforinger: Boolean = false,
)

suspend fun RoutingContext.getAdminTiltaksgjennomforingFilter(): AdminTiltaksgjennomforingFilter {
    val request = call.receive<GetGjennomforingerRequest>()
    val administratorNavIdent = request.visMineGjennomforinger.takeIf { it }?.let { getNavIdent() }

    return AdminTiltaksgjennomforingFilter(
        search = request.search,
        navEnheter = request.navEnheter,
        tiltakskoder = request.tiltakstyper,
        statuser = request.statuser,
        sortering = request.sort,
        avtaleId = request.avtaleId,
        arrangorIds = request.arrangorer,
        publisert = request.publisert,
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
data class GjenapneGjennomforingRequest(
    @Serializable(with = LocalDateSerializer::class)
    val nySluttDato: LocalDate,
)

@Serializable
data class TiltaksnummerResponse(
    val tiltaksnummer: String,
)

@Serializable
data class PublisertRequest(
    val publisert: Boolean,
)

@Serializable
data class SetEstimertVentetidRequest(
    val verdi: Int?,
    val enhet: String?,
) {
    fun validate(): Either<List<FieldError>, GjennomforingAvtaleDetaljer.EstimertVentetid?> = validation {
        if (verdi == null && enhet == null) {
            return@validation null
        }

        validate(verdi != null && verdi > 0) {
            FieldError.of("Du må velge en verdi større enn 0", SetEstimertVentetidRequest::verdi)
        }

        requireValid(!enhet.isNullOrBlank()) {
            FieldError.of("Du må velge en enhet", SetEstimertVentetidRequest::enhet)
        }

        GjennomforingAvtaleDetaljer.EstimertVentetid(requireNotNull(verdi), enhet)
    }
}

@Serializable
data class SetApentForPameldingRequest(
    val apentForPamelding: Boolean,
)
