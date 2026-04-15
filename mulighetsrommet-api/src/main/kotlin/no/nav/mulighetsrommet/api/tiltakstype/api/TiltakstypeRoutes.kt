package no.nav.mulighetsrommet.api.tiltakstype.api

import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import io.ktor.server.util.getValue
import no.nav.mulighetsrommet.api.navansatt.ktor.authorize
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.plugins.pathParameterUuid
import no.nav.mulighetsrommet.api.tiltakstype.model.TiltakstypeDto
import no.nav.mulighetsrommet.api.tiltakstype.model.TiltakstypeHandling
import no.nav.mulighetsrommet.api.tiltakstype.model.TiltakstypeKompaktDto
import no.nav.mulighetsrommet.api.tiltakstype.service.TiltakstypeDetaljerService
import no.nav.mulighetsrommet.model.ProblemDetail
import org.koin.ktor.ext.inject
import java.util.UUID

fun Route.tiltakstypeRoutes() {
    val tiltakstypeDetaljerService: TiltakstypeDetaljerService by inject()

    route("tiltakstyper") {
        get({
            tags = setOf("Tiltakstype")
            operationId = "getTiltakstyper"
            request {
                queryParameter<String>("sort")
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
            val filter = getTiltakstypeFilter()

            val tiltakstyper = tiltakstypeDetaljerService.getAll(filter)

            call.respond(tiltakstyper)
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

            val tiltakstype = tiltakstypeDetaljerService.getById(id) ?: return@get call.respondText(
                "Det finnes ikke noe tiltakstype med id $id",
                status = HttpStatusCode.NotFound,
            )

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
            val id: UUID by call.parameters
            val navIdent = getNavIdent()

            val handlinger = tiltakstypeDetaljerService.getHandlinger(id, navIdent)

            call.respond(handlinger)
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
                val request = call.receive<TiltakstypeVeilederinfoRequest>()

                val result = tiltakstypeDetaljerService.upsertRedaksjoneltInnhold(id, request) ?: return@post call.respondText(
                    "Det finnes ikke noe tiltakstype med id $id",
                    status = HttpStatusCode.NotFound,
                )

                call.respond(result)
            }
        }
    }
}
