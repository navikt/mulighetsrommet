@file:UseSerializers(UUIDSerializer::class)

package no.nav.mulighetsrommet.api.gjennomforing.api

import arrow.core.flatMap
import arrow.core.left
import arrow.core.nel
import arrow.core.right
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import io.ktor.server.util.getValue
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.mulighetsrommet.admin.gjennomforing.ArenaEnkeltplassDto
import no.nav.mulighetsrommet.admin.gjennomforing.ArenaEnkeltplassDtoQuery
import no.nav.mulighetsrommet.admin.gjennomforing.EnkeltplassBeslutningError
import no.nav.mulighetsrommet.admin.gjennomforing.FinnArenaEnkeltplassError
import no.nav.mulighetsrommet.admin.gjennomforing.FinnArenaEnkeltplassInput
import no.nav.mulighetsrommet.admin.gjennomforing.GjennomforingDetaljerError
import no.nav.mulighetsrommet.admin.gjennomforing.GjennomforingDetaljerService
import no.nav.mulighetsrommet.admin.gjennomforing.GjennomforingEnkeltplassService
import no.nav.mulighetsrommet.admin.gjennomforing.GjennomforingHandling
import no.nav.mulighetsrommet.admin.gjennomforing.MigrerEnkeltplass
import no.nav.mulighetsrommet.admin.gjennomforing.MigrerEnkeltplassError
import no.nav.mulighetsrommet.admin.gjennomforing.UpsertEnkeltplass
import no.nav.mulighetsrommet.admin.navansatt.NavAnsattContext
import no.nav.mulighetsrommet.api.domain.navansatt.Rolle
import no.nav.mulighetsrommet.api.domain.opplaring.Opplaeringtilskudd
import no.nav.mulighetsrommet.api.domain.tiltak.PrismodellType
import no.nav.mulighetsrommet.api.navansatt.ktor.authorize
import no.nav.mulighetsrommet.api.plugins.getAccessType
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.plugins.pathParameterUuid
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.responses.respondWithStatusResponse
import no.nav.mulighetsrommet.ktor.exception.Forbidden
import no.nav.mulighetsrommet.ktor.exception.StatusException
import no.nav.mulighetsrommet.ktor.exception.toProblemDetail
import no.nav.mulighetsrommet.model.ProblemDetail
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import no.nav.mulighetsrommet.tokenprovider.requireAzureAd
import no.nav.mulighetsrommet.validation.FieldError
import no.nav.mulighetsrommet.validation.Validated
import org.koin.ktor.ext.inject
import java.util.UUID

fun Route.enkeltplassRoutes() {
    val enkeltplasser: GjennomforingEnkeltplassService by inject()
    val detaljer: GjennomforingDetaljerService by inject()
    val finnArenaEnkeltplass: ArenaEnkeltplassDtoQuery by inject()

    route("enkeltplasser") {
        get("handlinger", {
            tags = setOf("Enkeltplass")
            operationId = "getEnkeltplassHandlinger"
            response {
                code(HttpStatusCode.OK) {
                    description = "Mulige handlinger på enkeltplasser for innlogget bruker"
                    body<Set<GjennomforingHandling>>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            call.respond(detaljer.getHandlinger(getNavIdent()))
        }

        authorize(Rolle.OKONOMI_SAKSBEHANDLER_ENKELTPLASS) {
            post("migrering/sok", {
                tags = setOf("Enkeltplass")
                operationId = "finnArenaEnkeltplass"
                request {
                    body<FinnArenaEnkeltplassRequest>()
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Arena-gjennomføringer som kan migreres"
                        body<List<ArenaEnkeltplassDto>>()
                    }
                    default {
                        description = "Problem details"
                        body<ProblemDetail>()
                    }
                }
            }) {
                val request = call.receive<FinnArenaEnkeltplassRequest>()
                val context = NavAnsattContext(getNavIdent(), call.getAccessType().requireAzureAd())

                val result = finnArenaEnkeltplass
                    .execute(FinnArenaEnkeltplassInput(request.search), context)
                    .mapLeft(::toProblemDetail)

                call.respondWithStatusResponse(result)
            }

            post("{id}/migrer", {
                tags = setOf("Enkeltplass")
                operationId = "migrerEnkeltplass"
                request {
                    pathParameterUuid("id")
                    body<MigrerEnkeltplassRequest>()
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Enkeltplass migrert med prisinformasjon"
                        body<MigrerEnkeltplassResponse>()
                    }
                    default {
                        description = "Problem details"
                        body<ProblemDetail>()
                    }
                }
            }) {
                val id: UUID by call.parameters
                val request = call.receive<MigrerEnkeltplassRequest>()
                val context = NavAnsattContext(getNavIdent(), call.getAccessType().requireAzureAd())

                val result = request.toCommand(id)
                    .mapLeft { ValidationError(errors = it) }
                    .flatMap { command -> enkeltplasser.migrerEnkeltplass(command, context).mapLeft(::toProblemDetail) }
                    .map { MigrerEnkeltplassResponse(it.gjennomforing.id) }

                call.respondWithStatusResponse(result)
            }
        }

        authorize(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS) {
            post("{id}/godkjenn-okonomi", {
                tags = setOf("Enkeltplass")
                operationId = "godkjennOkonomi"
                request {
                    pathParameterUuid("id")
                    body<GodkjennOkonomiRequest>()
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Økonomi ble godkjent"
                    }
                    default {
                        description = "Problem details"
                        body<ProblemDetail>()
                    }
                }
            }) {
                val id: UUID by call.parameters
                val request = call.receive<GodkjennOkonomiRequest>()
                val navIdent = getNavIdent()

                val result = enkeltplasser.settOkonomiGodkjent(id, request.totrinnskontrollId, navIdent)
                    .mapLeft(::toProblemDetail)
                    .map { HttpStatusCode.OK }

                call.respondWithStatusResponse(result)
            }

            post("{id}/sett-pa-vent-okonomi", {
                tags = setOf("Enkeltplass")
                operationId = "settOkonomiPaVent"
                request {
                    pathParameterUuid("id")
                    body<SettPaVentOkonomiRequest>()
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Økonomi ble satt på vent"
                    }
                    default {
                        description = "Problem details"
                        body<ProblemDetail>()
                    }
                }
            }) {
                val id: UUID by call.parameters
                val request = call.receive<SettPaVentOkonomiRequest>()
                val navIdent = getNavIdent()

                val result = enkeltplasser
                    .settOkonomiPaVent(id, request.totrinnskontrollId, navIdent, request.begrunnelse)
                    .mapLeft(::toProblemDetail)
                    .map { HttpStatusCode.OK }

                call.respondWithStatusResponse(result)
            }
        }
    }
}

@Serializable
data class FinnArenaEnkeltplassRequest(val search: String)

@Serializable
data class MigrerEnkeltplassRequest(
    val type: PrismodellType,
    val totalbelop: Int? = null,
    val prisbetingelser: String? = null,
    val tilskudd: Map<Opplaeringtilskudd.Kode, Int> = emptyMap(),
)

private fun MigrerEnkeltplassRequest.toCommand(id: UUID): Validated<MigrerEnkeltplass> {
    val prismodell = when (type) {
        PrismodellType.ANSKAFFET_ENKELTPLASS -> UpsertEnkeltplass.Prismodell.Anskaffelse(
            totalbelop ?: return FieldError.of(
                "Totalbeløp må være større enn 0",
                MigrerEnkeltplassRequest::totalbelop,
            ).nel().left(),
        )

        PrismodellType.TILSKUDD_TIL_OPPLAERING -> UpsertEnkeltplass.Prismodell.TilskuddTilOpplaering(
            tilskudd = tilskudd,
            prisbetingelser = prisbetingelser,
        )

        PrismodellType.INGEN_KOSTNADER -> UpsertEnkeltplass.Prismodell.IngenKostnader(prisbetingelser.orEmpty())

        else -> return FieldError.of(
            "Prismodellen er ikke støttet for enkeltplasser",
            MigrerEnkeltplassRequest::type,
        ).nel().left()
    }
    return MigrerEnkeltplass(id, prismodell).right()
}

@Serializable
data class MigrerEnkeltplassResponse(
    val id: UUID,
)

private fun toProblemDetail(error: MigrerEnkeltplassError): ProblemDetail = when (error) {
    is MigrerEnkeltplassError.Valideringsfeil -> ValidationError(errors = error.errors)
    is MigrerEnkeltplassError.ManglerTilgang -> toProblemDetail(GjennomforingDetaljerError.PersonaliaSjekkAvvist(error.avvistGrunn))
}

private fun toProblemDetail(error: FinnArenaEnkeltplassError): ProblemDetail = when (error) {
    is FinnArenaEnkeltplassError.Valideringsfeil -> ValidationError(errors = error.errors)

    is FinnArenaEnkeltplassError.ManglerTilgang -> toProblemDetail(
        GjennomforingDetaljerError.PersonaliaSjekkAvvist(error.avvistGrunn),
    )

    FinnArenaEnkeltplassError.OppslagFeilet -> StatusException(
        HttpStatusCode.BadGateway,
        "Kunne ikke hente deltakelser fra Komet",
    ).toProblemDetail()
}

private fun toProblemDetail(error: EnkeltplassBeslutningError): ProblemDetail = when (error) {
    is EnkeltplassBeslutningError.ManglerTilgang -> Forbidden(error.message)

    is EnkeltplassBeslutningError.Valideringsfeil -> ValidationError(errors = error.errors)

    is EnkeltplassBeslutningError.ForslagTrukketTilbake -> ValidationError(
        detail = error.detail,
        errors = listOf(FieldError.of(error.detail)),
    )
}

@Serializable
data class SettPaVentOkonomiRequest(
    val begrunnelse: String? = null,
    val totrinnskontrollId: UUID,
)

@Serializable
data class GodkjennOkonomiRequest(
    val totrinnskontrollId: UUID,
)
