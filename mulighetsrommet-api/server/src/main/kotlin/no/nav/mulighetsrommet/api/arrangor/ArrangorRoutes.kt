package no.nav.mulighetsrommet.api.arrangor

import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.route
import io.ktor.server.util.getOrFail
import io.ktor.server.util.getValue
import no.nav.mulighetsrommet.admin.arrangor.ArrangorDto
import no.nav.mulighetsrommet.admin.arrangor.ArrangorHovedenhetDto
import no.nav.mulighetsrommet.admin.arrangor.ArrangorKobling
import no.nav.mulighetsrommet.admin.arrangor.BetalingsinformasjonQuery
import no.nav.mulighetsrommet.admin.arrangor.HentBetalingsinformasjon
import no.nav.mulighetsrommet.admin.arrangor.SyncArrangorIfMissing
import no.nav.mulighetsrommet.admin.arrangor.SyncArrangorUseCase
import no.nav.mulighetsrommet.admin.arrangor.toDto
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.domain.arrangor.Betalingsinformasjon
import no.nav.mulighetsrommet.api.parameters.getPaginationParams
import no.nav.mulighetsrommet.api.plugins.pathParameterUuid
import no.nav.mulighetsrommet.api.responses.PaginatedResponse
import no.nav.mulighetsrommet.api.responses.respondWithStatusResponse
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.ProblemDetail
import org.koin.ktor.ext.inject
import java.util.UUID

fun Route.arrangorRoutes() {
    val db: ApiDatabase by inject()
    val syncArrangor: SyncArrangorUseCase by inject()
    val betalingsinformasjon: BetalingsinformasjonQuery by inject()

    route("arrangorer") {
        post("{orgnr}", {
            tags = setOf("Arrangor")
            operationId = "syncArrangorFromBrreg"
            request {
                pathParameter<Organisasjonsnummer>("orgnr")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Arrangør"
                    body<ArrangorDto>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val orgnr = call.parameters.getOrFail("orgnr").let { Organisasjonsnummer(it) }

            val response = syncArrangor.execute(SyncArrangorIfMissing(orgnr))
                .map { it.toDto() }
                .mapLeft { it.toProblemDetail() }

            call.respondWithStatusResponse(response)
        }

        get({
            tags = setOf("Arrangor")
            operationId = "getArrangorer"
            request {
                queryParameter<Int>("page")
                queryParameter<Int>("size")
                queryParameter<ArrangorKobling>("kobling")
                queryParameter<String>("sok")
                queryParameter<String>("sort")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Tilsagn i tabellformat"
                    body<PaginatedResponse<ArrangorDto>>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val filter = getArrangorFilter()
            val pagination = getPaginationParams()

            val (totalCount, items) = db.session {
                queries.arrangor.getAll(
                    kobling = filter.kobling,
                    sok = filter.sok,
                    sortering = filter.sortering,
                    pagination = pagination,
                )
            }

            call.respond(PaginatedResponse.of(pagination, totalCount, items))
        }

        get("{id}", {
            tags = setOf("Arrangor")
            operationId = "getArrangor"
            request {
                pathParameterUuid("id")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Arrangør"
                    body<ArrangorDto>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val id: UUID by call.parameters

            val arrangor = db.session { queries.arrangor.getById(id) }

            call.respond(arrangor)
        }

        get("{id}/betalingsinformasjon", {
            tags = setOf("Arrangor")
            operationId = "getBetalingsinformasjon"
            request {
                pathParameterUuid("id")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Betalingsinformasjon til arrangør"
                    body<Betalingsinformasjon>()
                }
                code(HttpStatusCode.NoContent) {
                    description = "Arrangør har ingen betalingsinformasjon registrert"
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val id: UUID by call.parameters

            val response = betalingsinformasjon.execute(HentBetalingsinformasjon(id))
                ?: return@get call.respond(HttpStatusCode.NoContent)

            call.respond(HttpStatusCode.OK, response)
        }

        get("{id}/hovedenhet", {
            tags = setOf("Arrangor")
            operationId = "getArrangorHovedenhet"
            request {
                pathParameterUuid("id")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Hovedenhet til arrangør"
                    body<ArrangorHovedenhetDto>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val id: UUID by call.parameters

            val arrangor = db.session { queries.arrangor.getHovedenhetById(id) }

            call.respond(arrangor)
        }
    }
}

data class ArrangorFilter(
    val kobling: ArrangorKobling? = null,
    val sok: String? = null,
    val sortering: String? = null,
)

fun RoutingContext.getArrangorFilter(): ArrangorFilter {
    val kobling = call.request.queryParameters["kobling"]
    val sok = call.request.queryParameters["sok"]
    val sortering = call.request.queryParameters["sortering"]
    return ArrangorFilter(
        kobling = kobling?.let { ArrangorKobling.valueOf(it) },
        sok = sok,
        sortering = sortering,
    )
}
