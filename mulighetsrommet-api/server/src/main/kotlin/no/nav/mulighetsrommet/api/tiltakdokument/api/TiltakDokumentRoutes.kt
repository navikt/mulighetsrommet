package no.nav.mulighetsrommet.api.tiltakdokument.api

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
import no.nav.mulighetsrommet.api.navansatt.ktor.authorize
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.plugins.pathParameterUuid
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.responses.respondWithStatusResponse
import no.nav.mulighetsrommet.api.tiltakdokument.model.TiltakDokument
import no.nav.mulighetsrommet.api.tiltakdokument.service.TiltakDokumentService
import no.nav.mulighetsrommet.model.Faneinnhold
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.ProblemDetail
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import org.koin.ktor.ext.inject
import java.util.UUID

@Serializable
enum class TiltakDokumentHandling {
    PUBLISER,
    REDIGER,
    FORHANDSVIS_I_MODIA,
}

@Serializable
data class TiltakDokumentPublisertRequest(
    val publisert: Boolean,
)

@Serializable
data class TiltakDokumentRequest(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val navn: String,
    @Serializable(with = UUIDSerializer::class)
    val tiltakstypeId: UUID,
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
data class GetTiltakDokumenterRequest(
    val navEnheter: List<NavEnhetNummer> = emptyList(),
    val tiltakstyper: List<Tiltakskode> = emptyList(),
)

fun Route.tiltakDokumentRoutes() {
    val service: TiltakDokumentService by inject()

    route("tiltak-dokumenter") {
        authorize(Rolle.TILTAKSGJENNOMFORINGER_SKRIV) {
            put({
                tags = setOf("TiltakDokument")
                operationId = "upsertTiltakDokument"
                request {
                    body<TiltakDokumentRequest>()
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Individuell gjennomføring ble opprettet/oppdatert"
                        body<TiltakDokument>()
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
                val request = call.receive<TiltakDokumentRequest>()
                val result = service.upsert(request).mapLeft { ValidationError(errors = it) }
                call.respondWithStatusResponse(result)
            }

            put("{id}/tilgjengelig-for-veileder", {
                tags = setOf("TiltakDokument")
                operationId = "setPublisertTiltakDokument"
                request {
                    pathParameterUuid("id")
                    body<TiltakDokumentPublisertRequest>()
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
                val request = call.receive<TiltakDokumentPublisertRequest>()
                service.setPublisert(id, request.publisert)
                call.respond(HttpStatusCode.OK)
            }
        }

        post("filter", {
            tags = setOf("TiltakDokument")
            operationId = "getTiltakDokumenter"
            request {
                body<GetTiltakDokumenterRequest>()
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Liste over tiltak-dokumentøringer"
                    body<List<TiltakDokument>>()
                }
            }
        }) {
            val request = call.receive<GetTiltakDokumenterRequest>()
            call.respond(
                service.getAll(
                    navEnheter = request.navEnheter.map { it.value },
                    tiltakstyper = request.tiltakstyper,
                ),
            )
        }

        get({
            tags = setOf("TiltakDokument")
            operationId = "getAllTiltakDokumenter"
            response {
                code(HttpStatusCode.OK) {
                    description = "Liste over tiltak-dokumentøringer (uten filter)"
                    body<List<TiltakDokument>>()
                }
            }
        }) {
            call.respond(service.getAll())
        }

        get("{id}", {
            tags = setOf("TiltakDokument")
            operationId = "getTiltakDokument"
            request {
                pathParameterUuid("id")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Individuell gjennomføring"
                    body<TiltakDokument>()
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
            tags = setOf("TiltakDokument")
            operationId = "getTiltakDokumentHandlinger"
            request {
                pathParameterUuid("id")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Mulige handlinger for innlogget bruker"
                    body<Set<TiltakDokumentHandling>>()
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
