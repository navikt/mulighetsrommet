package no.nav.mulighetsrommet.api.gjennomforing.api

import io.github.smiley4.ktoropenapi.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import io.ktor.server.util.getValue
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.domain.navansatt.Rolle
import no.nav.mulighetsrommet.api.gjennomforing.service.GjennomforingEnkeltplassService
import no.nav.mulighetsrommet.api.navansatt.ktor.authorize
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.plugins.pathParameterUuid
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.responses.respondWithStatusResponse
import no.nav.mulighetsrommet.model.ProblemDetail
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import org.koin.ktor.ext.inject
import java.util.UUID

fun Route.enkeltplassRoutes() {
    val enkeltplasser: GjennomforingEnkeltplassService by inject()

    route("enkeltplasser") {
        authorize(Rolle.BESLUTTER_TILSAGN) {
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
                    .mapLeft { ValidationError(errors = it) }
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
                    .settOkonomiPaVent(id, request.totrinnskontrollId, navIdent, request.forklaring)
                    .mapLeft { ValidationError(errors = it) }
                    .map { HttpStatusCode.OK }

                call.respondWithStatusResponse(result)
            }
        }
    }
}

@Serializable
data class SettPaVentOkonomiRequest(
    val forklaring: String? = null,
    @Serializable(with = UUIDSerializer::class)
    val totrinnskontrollId: UUID,
)

@Serializable
data class GodkjennOkonomiRequest(
    @Serializable(with = UUIDSerializer::class)
    val totrinnskontrollId: UUID,
)
