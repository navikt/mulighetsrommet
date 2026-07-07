package no.nav.mulighetsrommet.api.tiltakstype.api

import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import io.ktor.server.util.getValue
import no.nav.mulighetsrommet.api.application.AdminDatabase
import no.nav.mulighetsrommet.api.application.tiltak.GetTiltakstepeDto
import no.nav.mulighetsrommet.api.application.tiltak.TiltakstypeDto
import no.nav.mulighetsrommet.api.application.tiltak.TiltakstypeDtoQuery
import no.nav.mulighetsrommet.api.application.tiltak.TiltakstypeHandling
import no.nav.mulighetsrommet.api.application.tiltak.TiltakstypeKompaktDto
import no.nav.mulighetsrommet.api.application.tiltak.TiltakstypeKompaktQuery
import no.nav.mulighetsrommet.api.application.tiltak.TiltakstypeUseCase
import no.nav.mulighetsrommet.api.application.tiltak.TiltakstypeUseCaseError
import no.nav.mulighetsrommet.api.application.tiltak.UpsertDeltakerinfoCommand
import no.nav.mulighetsrommet.api.application.tiltak.UpsertVeilederinfoCommand
import no.nav.mulighetsrommet.api.domain.tiltak.SortDirection
import no.nav.mulighetsrommet.api.domain.tiltak.Tiltakstype
import no.nav.mulighetsrommet.api.domain.tiltak.TiltakstypeSortField
import no.nav.mulighetsrommet.api.navansatt.ktor.authorize
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.navansatt.service.NavAnsattService
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.plugins.pathParameterUuid
import no.nav.mulighetsrommet.api.responses.respondWithStatusResponse
import no.nav.mulighetsrommet.ktor.plugins.respondWithProblemDetail
import no.nav.mulighetsrommet.model.Innholdselement
import no.nav.mulighetsrommet.model.ProblemDetail
import no.nav.mulighetsrommet.model.TiltakstypeEgenskap
import org.koin.ktor.ext.inject
import java.util.UUID

fun Route.tiltakstypeRoutes() {
    val db: AdminDatabase by inject()
    val tiltakstypeKompaktQuery: TiltakstypeKompaktQuery by inject()
    val tiltakstypeDtoQuery: TiltakstypeDtoQuery by inject()
    val tiltakstypeUseCase: TiltakstypeUseCase by inject()
    val navAnsattService: NavAnsattService by inject()

    route("tiltakstyper") {
        get({
            tags = setOf("Tiltakstype")
            operationId = "getTiltakstyper"
            request {
                queryParameter<TiltakstypeSortField>("sortField")
                queryParameter<SortDirection>("sortDirection")
                queryParameter<Set<TiltakstypeEgenskap>>("egenskaper") {
                    explode = true
                }
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Tiltakstyper i Tiltaksadministrasjon"
                    body<List<TiltakstypeKompaktDto>>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val query = getTiltakstypeKompaktQuery()

            val result = tiltakstypeKompaktQuery.execute(query)

            call.respond(result)
        }

        get("{id}", {
            tags = setOf("Tiltakstype")
            operationId = "getTiltakstype"
            request {
                pathParameterUuid("id")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Tiltakstype"
                    body<TiltakstypeDto>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val id: UUID by call.parameters

            val tiltakstype = tiltakstypeDtoQuery.execute(GetTiltakstepeDto(id))
                ?: return@get call.respondWithProblemDetail(tiltakstypeNotFound(id))

            call.respond(tiltakstype)
        }

        get("{id}/handlinger", {
            tags = setOf("Tiltakstype")
            operationId = "getTiltakstypeHandlinger"
            request {
                pathParameterUuid("id")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Mulige handlinger på tiltakstype for innlogget bruker"
                    body<Set<TiltakstypeHandling>>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val navIdent = getNavIdent()

            val ansatt = navAnsattService.getNavAnsattByNavIdent(navIdent)
            val handlinger = setOfNotNull(
                TiltakstypeHandling.REDIGER_VEILEDERINFO.takeIf { ansatt?.hasGenerellRolle(Rolle.TILTAKSTYPER_SKRIV) == true },
                TiltakstypeHandling.REDIGER_DELTAKERINFO.takeIf { ansatt?.hasGenerellRolle(Rolle.TILTAKSTYPER_REDIGER_DELTAKERINFO) == true },
            )

            call.respond(handlinger)
        }

        get("innholdselementer", {
            tags = setOf("Tiltakstype")
            operationId = "getInnholdselementer"
            response {
                code(HttpStatusCode.OK) {
                    description = "Tilgjengelige innholdselementer for deltakerregistrering"
                    body<List<Innholdselement>>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val innholdselementer = db.session {
                queries.tiltakstype.getAllInnholdselementer()
            }

            call.respond(innholdselementer)
        }

        authorize(Rolle.TILTAKSTYPER_SKRIV) {
            post("{id}/veilederinfo", {
                tags = setOf("Tiltakstype")
                operationId = "updateVeilederinfo"
                request {
                    pathParameterUuid("id")
                    body<TiltakstypeVeilederinfoRequest>()
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Oppdatert tiltakstype"
                        body<TiltakstypeDto>()
                    }
                    code(HttpStatusCode.NotFound) {
                        description = "Tiltakstype ikke funnet"
                    }
                    default {
                        description = "Problem details"
                        body<ProblemDetail>()
                    }
                }
            }) {
                val id: UUID by call.parameters
                val navIdent = getNavIdent()
                val request = call.receive<TiltakstypeVeilederinfoRequest>()

                val command = UpsertVeilederinfoCommand(
                    id = id,
                    veilederinfo = Tiltakstype.Veilederinfo(
                        beskrivelse = request.beskrivelse,
                        faneinnhold = request.faneinnhold,
                        faglenker = request.faglenker,
                        kanKombineresMed = request.kanKombineresMed,
                    ),
                    endretAv = navIdent,
                )
                val result = tiltakstypeUseCase.execute(command)
                    .mapLeft { error ->
                        when (error) {
                            is TiltakstypeUseCaseError.NotFound -> tiltakstypeNotFound(error.id)
                        }
                    }
                    .flatMap {
                        tiltakstypeDtoQuery.execute(GetTiltakstepeDto(id))?.right() ?: tiltakstypeNotFound(id).left()
                    }

                call.respondWithStatusResponse(result)
            }
        }

        authorize(Rolle.TILTAKSTYPER_REDIGER_DELTAKERINFO) {
            post("{id}/deltakerinfo", {
                tags = setOf("Tiltakstype")
                operationId = "updateDeltakerinfo"
                request {
                    pathParameterUuid("id")
                    body<TiltakstypeDeltakerinfoRequest>()
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Oppdatert tiltakstype"
                        body<TiltakstypeDto>()
                    }
                    code(HttpStatusCode.NotFound) {
                        description = "Tiltakstype ikke funnet"
                    }
                    default {
                        description = "Problem details"
                        body<ProblemDetail>()
                    }
                }
            }) {
                val id: UUID by call.parameters
                val navIdent = getNavIdent()
                val request = call.receive<TiltakstypeDeltakerinfoRequest>()

                val command = UpsertDeltakerinfoCommand(
                    id = id,
                    deltakerinfo = Tiltakstype.Deltakerinfo(
                        ledetekst = request.ledetekst,
                        innholdskoder = request.innholdskoder,
                    ),
                    endretAv = navIdent,
                )
                val result = tiltakstypeUseCase.execute(command)
                    .mapLeft { error ->
                        when (error) {
                            is TiltakstypeUseCaseError.NotFound -> tiltakstypeNotFound(error.id)
                        }
                    }
                    .flatMap {
                        tiltakstypeDtoQuery.execute(GetTiltakstepeDto(id))?.right() ?: tiltakstypeNotFound(id).left()
                    }

                call.respondWithStatusResponse(result)
            }
        }
    }
}
