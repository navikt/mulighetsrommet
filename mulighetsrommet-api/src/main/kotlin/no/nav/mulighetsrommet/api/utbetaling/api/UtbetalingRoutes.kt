package no.nav.mulighetsrommet.api.utbetaling.api

import arrow.core.flatMap
import io.github.smiley4.ktoropenapi.delete
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.github.smiley4.ktoropenapi.put
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.route
import io.ktor.server.util.getValue
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.MrExceptions
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.aarsakerforklaring.AarsakerOgForklaringRequest
import no.nav.mulighetsrommet.api.endringshistorikk.DocumentClass
import no.nav.mulighetsrommet.api.endringshistorikk.EndringshistorikkDto
import no.nav.mulighetsrommet.api.navansatt.ktor.authorize
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsatt
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.navenhet.Kontorstruktur
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.plugins.pathParameterUuid
import no.nav.mulighetsrommet.api.plugins.queryParameterUuid
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.responses.respondWithStatusResponse
import no.nav.mulighetsrommet.api.tilsagn.api.KostnadsstedDto
import no.nav.mulighetsrommet.api.tilsagn.api.TilsagnDto
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.api.totrinnskontroll.api.toDto
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.api.utbetaling.DeltakerPersonaliaMedGeografiskEnhet
import no.nav.mulighetsrommet.api.utbetaling.PersonaliaService
import no.nav.mulighetsrommet.api.utbetaling.UtbetalingService
import no.nav.mulighetsrommet.api.utbetaling.UtbetalingValidator
import no.nav.mulighetsrommet.api.utbetaling.api.ValutaBelopRequest
import no.nav.mulighetsrommet.api.utbetaling.model.Delutbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingReturnertAarsak
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningOutputDeltakelse
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType
import no.nav.mulighetsrommet.ktor.plugins.respondWithProblemDetail
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.ProblemDetail
import no.nav.mulighetsrommet.model.ValutaBelop
import no.nav.mulighetsrommet.model.withValuta
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import org.koin.ktor.ext.inject
import java.time.LocalDate
import java.util.UUID

fun Route.utbetalingRoutes() {
    val db: ApiDatabase by inject()
    val utbetalingService: UtbetalingService by inject()
    val personaliaService: PersonaliaService by inject()

    get("/utbetaling", {
        description = "Hent alle utbetalinger for gitt gjennomføring"
        tags = setOf("Utbetaling")
        operationId = "getUtbetalinger"
        request {
            queryParameterUuid("gjennomforingId") {
                required = true
            }
        }
        response {
            code(HttpStatusCode.OK) {
                description = "Alle utbetalinger for gitt gjennomføring"
                body<List<UtbetalingKompaktDto>>()
            }
            default {
                description = "Problem details"
                body<ProblemDetail>()
            }
        }
    }) {
        val gjennomforingId: UUID by call.queryParameters

        val utbetalinger = db.session {
            queries.utbetaling.getByGjennomforing(gjennomforingId).map { utbetaling ->
                val delutbetalinger = queries.delutbetaling.getByUtbetalingId(utbetaling.id)

                val (belopUtbetalt, kostnadssteder) = when (utbetaling.status) {
                    UtbetalingStatusType.FERDIG_BEHANDLET,
                    UtbetalingStatusType.DELVIS_UTBETALT,
                    UtbetalingStatusType.UTBETALT,
                    ->
                        Pair(
                            delutbetalinger.sumOf {
                                it.pris.belop
                            }.withValuta(utbetaling.valuta),
                            delutbetalinger.map { delutbetaling ->
                                queries.tilsagn.getOrError(delutbetaling.tilsagnId).kostnadssted
                            }.distinct(),
                        )

                    UtbetalingStatusType.GENERERT,
                    UtbetalingStatusType.INNSENDT,
                    UtbetalingStatusType.TIL_ATTESTERING,
                    UtbetalingStatusType.RETURNERT,
                    UtbetalingStatusType.AVBRUTT,
                    -> (null to emptyList())
                }

                UtbetalingKompaktDto(
                    id = utbetaling.id,
                    status = UtbetalingStatusDto.fromUtbetalingStatus(utbetaling.status),
                    periode = utbetaling.periode,
                    kostnadssteder = kostnadssteder.map { KostnadsstedDto.fromNavEnhetDbo(it) },
                    belopUtbetalt = belopUtbetalt,
                    type = UtbetalingType.from(utbetaling).toDto(),
                )
            }
        }

        call.respond(utbetalinger)
    }

    get("/innsendinger", {
        description = "Hent filtrerte innsendinger"
        tags = setOf("Utbetaling")
        operationId = "getInnsendinger"
        request {
            queryParameter<List<String>>("tiltakstyper") {
                explode = true
            }
            queryParameter<List<NavEnhetNummer>>("navEnheter") {
                explode = true
            }
            queryParameter<String>("sort")
        }
        response {
            code(HttpStatusCode.OK) {
                description = "Alle innsendinger for gitte filtre"
                body<List<InnsendingKompaktDto>>()
            }
            default {
                description = "Problem details"
                body<ProblemDetail>()
            }
        }
    }) {
        val filter = getAdminInnsendingerFilter()

        val innsendinger = db.session {
            queries.utbetaling.getAll(filter)
        }

        call.respond(innsendinger)
    }

    route("/utbetaling/{id}") {
        authorize(anyOf = setOf(Rolle.OKONOMI_LES, Rolle.SAKSBEHANDLER_OKONOMI, Rolle.ATTESTANT_UTBETALING)) {
            get({
                description = "Hent detaljer om utbetaling"
                tags = setOf("Utbetaling")
                operationId = "getUtbetaling"
                request {
                    pathParameterUuid("id")
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Detaljer om utbetaling"
                        body<UtbetalingDetaljerDto>()
                    }
                    default {
                        description = "Problem details"
                        body<ProblemDetail>()
                    }
                }
            }) {
                val id: UUID by call.parameters

                val navIdent = getNavIdent()

                val utbetaling = db.session {
                    val ansatt =
                        queries.ansatt.getByNavIdent(navIdent) ?: throw MrExceptions.navAnsattNotFound(navIdent)
                    val utbetaling = queries.utbetaling.getOrError(id)

                    UtbetalingDetaljerDto(
                        utbetaling = UtbetalingDto.fromUtbetaling(utbetaling),
                        handlinger = UtbetalingService.utbetalingHandlinger(utbetaling, ansatt),
                    )
                }
                call.respond(utbetaling)
            }
        }

        authorize(Rolle.SAKSBEHANDLER_OKONOMI) {
            delete({
                description = "Slett korreksjon"
                tags = setOf("Utbetaling")
                operationId = "slettKorreksjon"
                request {
                    pathParameterUuid("id")
                }
                response {
                    code(HttpStatusCode.OK) {}
                    default {
                        description = "Problem details"
                        body<ProblemDetail>()
                    }
                }
            }) {
                val id: UUID by call.parameters

                utbetalingService.slettKorreksjon(id)
                    .onLeft { call.respondWithProblemDetail(ValidationError(errors = it)) }
                    .onRight { call.respond(HttpStatusCode.OK) }
            }
        }

        authorize(anyOf = setOf(Rolle.OKONOMI_LES, Rolle.SAKSBEHANDLER_OKONOMI, Rolle.ATTESTANT_UTBETALING)) {
            get("/beregning", {
                tags = setOf("Utbetaling")
                operationId = "getUtbetalingBeregning"
                request {
                    pathParameterUuid("id")
                    queryParameter<List<String>>("navEnheter") {
                        explode = true
                    }
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Utbetalingen ble opprettet"
                        body<UtbetalingBeregningDto>()
                    }
                    default {
                        description = "Problem details"
                        body<ProblemDetail>()
                    }
                }
            }) {
                val id: UUID by call.parameters
                val filter = getBeregningFilter()

                val beregning = db.session {
                    val utbetaling = queries.utbetaling.getOrError(id)
                    val deltakelser = utbetaling.beregning.deltakelsePerioder().associateBy { it.deltakelseId }

                    val personalia = personaliaService.getPersonaliaMedGeografiskEnhet(deltakelser.keys)

                    val enheter = personalia.flatMapTo(mutableSetOf()) { listOfNotNull(it.oppfolgingEnhet, it.region) }
                    val kontorstruktur = Kontorstruktur.fromNavEnheter(enheter.toList())

                    val deltakelsePersoner = personalia
                        .filter { filter.navEnheter.isEmpty() || it.oppfolgingEnhet?.enhetsnummer in filter.navEnheter }
                        .associateBy { it.deltakerId }

                    UtbetalingBeregningDto.from(
                        utbetaling.beregning,
                        deltakelsePersoner,
                        kontorstruktur,
                        utbetalingPeriode = utbetaling.periode,
                    )
                }

                call.respond(beregning)
            }
        }

        get("/historikk", {
            tags = setOf("Utbetaling")
            operationId = "getUtbetalingEndringshistorikk"
            request {
                pathParameterUuid("id")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Utbetalingen ble opprettet"
                    body<EndringshistorikkDto>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val id: UUID by call.parameters
            val historikk = db.session {
                queries.endringshistorikk.getEndringshistorikk(DocumentClass.UTBETALING, id)
            }
            call.respond(historikk)
        }

        get("/tilsagn", {
            tags = setOf("Utbetaling")
            operationId = "getTilsagnTilUtbetaling"
            request {
                pathParameterUuid("id")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Tilsagn til utbetaling"
                    body<List<TilsagnDto>>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val id: UUID by call.parameters

            val tilsagn = db.session {
                val utbetaling = queries.utbetaling.get(id) ?: return@get call.respond(HttpStatusCode.NotFound)

                queries.tilsagn.getAll(
                    gjennomforingId = utbetaling.gjennomforing.id,
                    periodeIntersectsWith = utbetaling.periode,
                    typer = TilsagnType.fromTilskuddstype(utbetaling.tilskuddstype),
                ).map { TilsagnDto.fromTilsagn(it) }
            }

            call.respond(tilsagn)
        }

        authorize(anyOf = setOf(Rolle.OKONOMI_LES, Rolle.SAKSBEHANDLER_OKONOMI, Rolle.ATTESTANT_UTBETALING)) {
            get("/linjer", {
                tags = setOf("Utbetaling")
                operationId = "getUtbetalingsLinjer"
                request {
                    pathParameterUuid("id")
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Utbetalingslinjer til utbetaling"
                        body<List<UtbetalingLinje>>()
                    }
                    default {
                        description = "Problem details"
                        body<ProblemDetail>()
                    }
                }
            }) {
                val id: UUID by call.parameters
                val navIdent = getNavIdent()

                val utbetalingsLinjer = db.session {
                    val utbetaling = queries.utbetaling.getOrError(id)
                    val ansatt = queries.ansatt.getByNavIdent(navIdent)
                        ?: throw MrExceptions.navAnsattNotFound(navIdent)

                    val linjer = queries.delutbetaling.getByUtbetalingId(id)
                        .map { delutbetalingToUtbetalingLinje(it, ansatt) }

                    val nyeLinjer = queries.tilsagn
                        .getAll(
                            statuser = listOf(TilsagnStatus.GODKJENT),
                            gjennomforingId = utbetaling.gjennomforing.id,
                            periodeIntersectsWith = utbetaling.periode,
                            typer = TilsagnType.fromTilskuddstype(utbetaling.tilskuddstype),
                            valuta = utbetaling.valuta,
                        )
                        .filter { tilsagn -> linjer.none { it.tilsagn.id == tilsagn.id } }
                        .map {
                            UtbetalingLinje(
                                id = UUID.randomUUID(),
                                tilsagn = TilsagnDto.fromTilsagn(it),
                                status = null,
                                pris = 0.withValuta(utbetaling.valuta),
                                gjorOppTilsagn = false,
                                opprettelse = null,
                                handlinger = emptySet(),
                            )
                        }

                    (linjer + nyeLinjer).sortedBy { it.tilsagn.bestillingsnummer }
                }

                call.respond(utbetalingsLinjer)
            }
        }

        authorize(Rolle.SAKSBEHANDLER_OKONOMI) {
            post("/opprett-utbetaling", {
                tags = setOf("Utbetaling")
                operationId = "opprettUtbetaling"
                request {
                    pathParameterUuid("id")
                    body<OpprettUtbetalingRequest>()
                }
                response {
                    code(HttpStatusCode.Created) {
                        description = "Utbetalingen ble opprettet"
                    }
                    default {
                        description = "Problem details"
                        body<ProblemDetail>()
                    }
                }
            }) {
                val id: UUID by call.parameters
                val request = call.receive<OpprettUtbetalingRequest>()
                val navIdent = getNavIdent()

                val result = UtbetalingValidator.validateOpprettUtbetalingRequest(id, request)
                    .flatMap { utbetalingService.opprettAnnenAvtaltPrisUtbetaling(it, navIdent) }
                    .mapLeft { ValidationError("Klarte ikke opprette utbetaling", it) }
                    .map { HttpStatusCode.Created }

                call.respondWithStatusResponse(result)
            }
        }
    }

    route("/delutbetalinger") {
        authorize(Rolle.SAKSBEHANDLER_OKONOMI) {
            put({
                tags = setOf("Utbetaling")
                operationId = "opprettDelutbetalinger"
                request {
                    pathParameterUuid("id")
                    body<OpprettDelutbetalingerRequest>()
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Tilsanget ble sendt til oppgjør"
                    }
                    default {
                        description = "Problem details"
                        body<ProblemDetail>()
                    }
                }
            }) {
                val request = call.receive<OpprettDelutbetalingerRequest>()
                val navIdent = getNavIdent()

                val result = utbetalingService.opprettDelutbetalinger(request, navIdent)
                    .mapLeft { ValidationError(errors = it) }
                    .map { HttpStatusCode.OK }

                call.respondWithStatusResponse(result)
            }
        }

        authorize(Rolle.ATTESTANT_UTBETALING) {
            post("/{id}/attester", {
                tags = setOf("Utbetaling")
                operationId = "attesterDelutbetaling"
                request {
                    pathParameterUuid("id")
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Delutbetaling ble attestert"
                    }
                    default {
                        description = "Problem details"
                        body<ProblemDetail>()
                    }
                }
            }) {
                val id: UUID by call.parameters
                val navIdent = getNavIdent()

                val result = utbetalingService.godkjennDelutbetaling(id, navIdent)
                    .mapLeft { ValidationError(errors = it) }
                    .map { HttpStatusCode.OK }

                call.respondWithStatusResponse(result)
            }

            post("/{id}/returner", {
                tags = setOf("Utbetaling")
                operationId = "returnerDelutbetaling"
                request {
                    pathParameterUuid("id")
                    body<AarsakerOgForklaringRequest<DelutbetalingReturnertAarsak>>()
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Delutbetaling ble besluttet"
                    }
                    default {
                        description = "Problem details"
                        body<ProblemDetail>()
                    }
                }
            }) {
                val id: UUID by call.parameters
                val request = call.receive<AarsakerOgForklaringRequest<DelutbetalingReturnertAarsak>>()
                val navIdent = getNavIdent()

                val result = request.validate()
                    .flatMap { utbetalingService.returnerDelutbetaling(id, it.aarsaker, it.forklaring, navIdent) }
                    .mapLeft { ValidationError(errors = it) }
                    .map { HttpStatusCode.OK }

                call.respondWithStatusResponse(result)
            }
        }
    }
}

private fun QueryContext.delutbetalingToUtbetalingLinje(
    delutbetaling: Delutbetaling,
    navAnsatt: NavAnsatt,
): UtbetalingLinje {
    val tilsagn = queries.tilsagn.getOrError(delutbetaling.tilsagnId)

    val opprettelse = queries.totrinnskontroll
        .getOrError(delutbetaling.id, Totrinnskontroll.Type.OPPRETT)

    return UtbetalingLinje(
        id = delutbetaling.id,
        gjorOppTilsagn = delutbetaling.gjorOppTilsagn,
        pris = delutbetaling.pris,
        status = DelutbetalingStatusDto.fromDelutbetalingStatus(delutbetaling.status),
        tilsagn = TilsagnDto.fromTilsagn(tilsagn),
        opprettelse = opprettelse.toDto(),
        handlinger = UtbetalingService.linjeHandlinger(
            delutbetaling,
            opprettelse,
            tilsagn.kostnadssted.enhetsnummer,
            navAnsatt,
        ),
    )
}

data class AdminInnsendingerFilter(
    val navEnheter: List<NavEnhetNummer> = emptyList(),
    val tiltakstyper: List<UUID> = emptyList(),
    val sortering: String? = null,
)

fun RoutingContext.getAdminInnsendingerFilter(): AdminInnsendingerFilter {
    val navEnheter = call.parameters.getAll("navEnheter")?.map { NavEnhetNummer(it) } ?: emptyList()
    val tiltakstypeIder = call.parameters.getAll("tiltakstyper")?.map { UUID.fromString(it) } ?: emptyList()
    val sortering = call.request.queryParameters["sort"]

    return AdminInnsendingerFilter(
        navEnheter = navEnheter,
        tiltakstyper = tiltakstypeIder,
        sortering = sortering,
    )
}

@Serializable
data class DelutbetalingRequest(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = UUIDSerializer::class)
    val tilsagnId: UUID,
    val pris: ValutaBelop?,
    val gjorOppTilsagn: Boolean,
)

@Serializable
data class OpprettDelutbetalingerRequest(
    @Serializable(with = UUIDSerializer::class)
    val utbetalingId: UUID,
    val delutbetalinger: List<DelutbetalingRequest>,
    val begrunnelseMindreBetalt: String?,
)

@Serializable
data class OpprettUtbetalingRequest(
    @Serializable(with = UUIDSerializer::class)
    val gjennomforingId: UUID,
    @Serializable(with = LocalDateSerializer::class)
    val periodeStart: LocalDate? = null,
    @Serializable(with = LocalDateSerializer::class)
    val periodeSlutt: LocalDate? = null,
    val beskrivelse: String? = null,
    val kidNummer: String? = null,
    val pris: ValutaBelopRequest? = null,
)

data class BeregningFilter(
    val navEnheter: List<NavEnhetNummer>,
)

fun RoutingContext.getBeregningFilter() = BeregningFilter(
    navEnheter = call.parameters.getAll("navEnheter")?.map { NavEnhetNummer(it) } ?: emptyList(),
)

data class UtbetalingBeregningDeltaker(
    val personalia: DeltakerPersonaliaMedGeografiskEnhet,
    val deltakelse: UtbetalingBeregningOutputDeltakelse,
)
