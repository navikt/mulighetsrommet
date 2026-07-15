package no.nav.mulighetsrommet.api.individuell_gjennomforing.api

import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.github.smiley4.ktoropenapi.put
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import io.ktor.server.util.getValue
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.domain.navansatt.Rolle
import no.nav.mulighetsrommet.api.individuell_gjennomforing.model.IndividuellGjennomforing
import no.nav.mulighetsrommet.api.individuell_gjennomforing.service.IndividuellGjennomforingService
import no.nav.mulighetsrommet.api.navansatt.ktor.authorize
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.plugins.pathParameterUuid
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.responses.respondWithStatusResponse
import no.nav.mulighetsrommet.model.Faneinnhold
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.ProblemDetail
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import org.koin.ktor.ext.inject
import java.util.UUID

@Serializable
enum class IndividuellGjennomforingHandling {
    PUBLISER,
    REDIGER,
    FORHANDSVIS_I_MODIA,
}

@Serializable
data class IndividuellGjennomforingPublisertRequest(
    val publisert: Boolean,
)

@Serializable
data class IndividuellGjennomforingRequest(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val navn: String,
    @Serializable(with = UUIDSerializer::class)
    val tiltakstypeId: UUID? = null,
    val stedForGjennomforing: String? = null,
    @Serializable(with = UUIDSerializer::class)
    val arrangorId: UUID? = null,
    val arrangorKontaktpersoner: Set<
        @Serializable(with = UUIDSerializer::class)
        UUID,
        > = emptySet(),
    val faneinnhold: Faneinnhold? = null,
    val beskrivelse: String? = null,
    val administratorer: Set<NavIdent> = emptySet(),
    val navRegioner: Set<NavEnhetNummer> = emptySet(),
    val navKontorer: Set<NavEnhetNummer> = emptySet(),
    val navAndreEnheter: Set<NavEnhetNummer> = emptySet(),
    val kontaktpersoner: Set<Kontaktperson> = emptySet(),
) {
    @Serializable
    data class Kontaktperson(
        val navIdent: NavIdent,
        val beskrivelse: String? = null,
    )
}

@Serializable
data class GetIndividuelleGjennomforingerRequest(
    val navEnheter: List<NavEnhetNummer> = emptyList(),
    val tiltakstyper: List<Tiltakskode> = emptyList(),
)

fun Route.individuellGjennomforingRoutes() {
    val service: IndividuellGjennomforingService by inject()

    route("individuelle-gjennomforinger") {
        authorize(Rolle.TILTAKSGJENNOMFORINGER_SKRIV) {
            put({
                tags = setOf("IndividuellGjennomforing")
                operationId = "upsertIndividuellGjennomforing"
                request {
                    body<IndividuellGjennomforingRequest>()
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Individuell gjennomføring ble opprettet/oppdatert"
                        body<IndividuellGjennomforing>()
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
                val request = call.receive<IndividuellGjennomforingRequest>()
                val result = service.upsert(request).mapLeft { ValidationError(errors = it) }
                call.respondWithStatusResponse(result)
            }

            put("{id}/tilgjengelig-for-veileder", {
                tags = setOf("IndividuellGjennomforing")
                operationId = "setPublisertIndividuellGjennomforing"
                request {
                    pathParameterUuid("id")
                    body<IndividuellGjennomforingPublisertRequest>()
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
                val request = call.receive<IndividuellGjennomforingPublisertRequest>()
                service.setPublisert(id, request.publisert)
                call.respond(HttpStatusCode.OK)
            }
        }

        post("filter", {
            tags = setOf("IndividuellGjennomforing")
            operationId = "getIndividuelleGjennomforinger"
            request {
                body<GetIndividuelleGjennomforingerRequest>()
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Liste over individuelle gjennomføringer"
                    body<List<IndividuellGjennomforing>>()
                }
            }
        }) {
            val request = call.receive<GetIndividuelleGjennomforingerRequest>()
            call.respond(
                service.getAll(
                    navEnheter = request.navEnheter.map { it.value },
                    tiltakstyper = request.tiltakstyper,
                ),
            )
        }

        get({
            tags = setOf("IndividuellGjennomforing")
            operationId = "getAllIndividuelleGjennomforinger"
            response {
                code(HttpStatusCode.OK) {
                    description = "Liste over individuelle gjennomføringer (uten filter)"
                    body<List<IndividuellGjennomforing>>()
                }
            }
        }) {
            call.respond(service.getAll())
        }

        get("{id}", {
            tags = setOf("IndividuellGjennomforing")
            operationId = "getIndividuellGjennomforing"
            request {
                pathParameterUuid("id")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Individuell gjennomføring"
                    body<IndividuellGjennomforing>()
                }
                code(HttpStatusCode.NotFound) {
                    description = "Ikke funnet"
                    body<ProblemDetail>()
                }
            }
        }) {
            val id: UUID by call.parameters
            val gjennomforing = service.get(id)

            if (gjennomforing == null) {
                call.respond(HttpStatusCode.NotFound)
            } else {
                call.respond(gjennomforing)
            }
        }

        get("{id}/handlinger", {
            tags = setOf("IndividuellGjennomforing")
            operationId = "getIndividuellGjennomforingHandlinger"
            request {
                pathParameterUuid("id")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Mulige handlinger for innlogget bruker"
                    body<Set<IndividuellGjennomforingHandling>>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val id: UUID by call.parameters
            val navIdent = getNavIdent()
            call.respond(service.getHandlinger(id, navIdent))
        }
    }
}
