package no.nav.mulighetsrommet.api.tilskuddbehandling.api

import arrow.core.flatMap
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.log
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.route
import io.ktor.server.util.getOrFail
import io.ktor.server.util.getValue
import no.nav.mulighetsrommet.api.aarsakerforklaring.AarsakerOgForklaringRequest
import no.nav.mulighetsrommet.api.domain.navansatt.Rolle
import no.nav.mulighetsrommet.api.navansatt.ktor.authorize
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.plugins.pathParameterUuid
import no.nav.mulighetsrommet.api.plugins.queryParameterUuid
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.responses.respondWithStatusResponse
import no.nav.mulighetsrommet.api.tilskuddbehandling.TilskuddBehandlingService
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingDetaljerDto
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingDto
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingKompakt
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingRequest
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingStatusAarsak
import no.nav.mulighetsrommet.ktor.exception.InternalServerError
import no.nav.mulighetsrommet.ktor.plugins.respondWithProblemDetail
import no.nav.mulighetsrommet.model.ProblemDetail
import org.koin.ktor.ext.inject
import java.util.UUID

fun Route.tilskuddBehandlingRoutes() {
    val service: TilskuddBehandlingService by inject()

    route("tilskudd-behandling") {
        authorize(Rolle.SAKSBEHANDLER_OKONOMI) {
            get("/{tilskuddBehandlingId}", {
                description = "Hent tilskuddsbehandling gitt id"
                tags = setOf("TilskuddBehandling")
                operationId = "getTilskuddBehandling"
                request {
                    pathParameterUuid("tilskuddBehandlingId")
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Tilskuddsbehandling"
                        body<TilskuddBehandlingDetaljerDto>()
                    }
                    default {
                        description = "Problem details"
                        body<ProblemDetail>()
                    }
                }
            }) {
                val tilskuddBehandlingId = call.parameters.getOrFail<UUID>("tilskuddBehandlingId")
                val navIdent = getNavIdent()
                val result = service.getDetaljerDto(tilskuddBehandlingId, navIdent)
                    ?: return@get call.respond(HttpStatusCode.NotFound)
                call.respond(result)
            }

            get({
                description = "Hent alle tilskuddsbehandlinger for en gjennomføring"
                tags = setOf("TilskuddBehandling")
                operationId = "getTilskuddBehandlingerByGjennomforingId"
                request {
                    queryParameterUuid("gjennomforingId")
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Tilskuddsbehandlinger"
                        body<List<TilskuddBehandlingKompakt>>()
                    }
                    default {
                        description = "Problem details"
                        body<ProblemDetail>()
                    }
                }
            }) {
                val gjennomforingId: UUID by call.queryParameters
                val result = service.getByGjennomforingId(gjennomforingId)
                call.respond(result)
            }

            post({
                description = "Opprett tilskudd behandling"
                tags = setOf("TilskuddBehandling")
                operationId = "opprettTilskuddBehandling"
                request {
                    body<TilskuddBehandlingRequest>()
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Tilskudd behandling opprettet"
                        body<TilskuddBehandlingDto>()
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
                val request = call.receive<TilskuddBehandlingRequest>()
                val navIdent = getNavIdent()

                val result = service.upsert(request, navIdent)
                    .mapLeft { ValidationError(errors = it) }

                call.respondWithStatusResponse(result)
            }
        }

        authorize(Rolle.BESLUTTER_TILSAGN) {
            post("/{id}/attester", {
                tags = setOf("TilskuddBehandling")
                operationId = "attesterTilskuddBehandling"
                request {
                    pathParameterUuid("id")
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Tilskuddsbehandling ble attestert"
                    }
                    default {
                        description = "Problem details"
                        body<ProblemDetail>()
                    }
                }
            }) {
                val id = call.parameters.getOrFail<UUID>("id")
                val navIdent = getNavIdent()

                val result = service.attester(id, navIdent)
                    .mapLeft { ValidationError(errors = it) }
                    .map { HttpStatusCode.OK }

                call.respondWithStatusResponse(result)
            }
        }

        authorize(anyOf = setOf(Rolle.BESLUTTER_TILSAGN, Rolle.SAKSBEHANDLER_OKONOMI)) {
            post("/{id}/returner", {
                tags = setOf("TilskuddBehandling")
                operationId = "returnerTilskuddBehandling"
                request {
                    pathParameterUuid("id")
                    body<AarsakerOgForklaringRequest<TilskuddBehandlingStatusAarsak>>()
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Tilskuddsbehandling ble returnert"
                    }
                    default {
                        description = "Problem details"
                        body<ProblemDetail>()
                    }
                }
            }) {
                val id = call.parameters.getOrFail<UUID>("id")
                val request = call.receive<AarsakerOgForklaringRequest<TilskuddBehandlingStatusAarsak>>()
                val navIdent = getNavIdent()

                val result = request.validate()
                    .flatMap { service.returner(id, navIdent, it.aarsaker, it.forklaring) }
                    .mapLeft { ValidationError(errors = it) }
                    .map { HttpStatusCode.OK }

                call.respondWithStatusResponse(result)
            }

            get("/{id}/vedtaksbrev-pdf", {
                description = "Forhåndsvisning av vedtaksbrev pdf"
                tags = setOf("TilskuddBehandling")
                operationId = "getTilskuddBehandlingVedtaksbrevPdf"
                request {
                    pathParameterUuid("id")
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Vedtaksbrev-pdf"
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
                val id: UUID by call.parameters

                service.vedtaksbrevForhandsvisPdf(id)
                    .onRight { pdfContent ->
                        call.response.headers.append(
                            "Content-Disposition",
                            "attachment; filename=\"vedtaksbrev.pdf\"",
                        )
                        call.respondBytes(pdfContent, contentType = ContentType.Application.Pdf)
                    }
                    .onLeft { error ->
                        application.log.warn("Klarte ikke lage PDF. Response fra pdfgen: $error")
                        call.respondWithProblemDetail(InternalServerError("Klarte ikke lage PDF"))
                    }
            }

            post("vedtaksbrev-pdf", {
                description = "Forhåndsvisning av vedtaksbrev pdf"
                tags = setOf("TilskuddBehandling")
                operationId = "postTilskuddBehandlingVedtaksbrevPdf"
                request {
                    body<TilskuddBehandlingRequest>()
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Vedtaksbrev-pdf"
                        body<String> {
                            mediaTypes(ContentType.Application.Pdf)
                        }
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
                val request = call.receive<TilskuddBehandlingRequest>()

                service.vedtaksbrevForhandsvisPdf(request)
                    .onRight { pdfContent ->
                        call.response.headers.append(
                            "Content-Disposition",
                            "attachment; filename=\"vedtaksbrev.pdf\"",
                        )
                        call.respondBytes(pdfContent, contentType = ContentType.Application.Pdf)
                    }
                    .onLeft {
                        call.respondWithProblemDetail(ValidationError(errors = it))
                    }
            }
        }
    }
}
