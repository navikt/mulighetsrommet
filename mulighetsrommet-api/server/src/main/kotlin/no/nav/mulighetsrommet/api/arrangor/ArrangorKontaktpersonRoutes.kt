package no.nav.mulighetsrommet.api.arrangor

import io.github.smiley4.ktoropenapi.delete
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.put
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.log
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.route
import io.ktor.server.util.getValue
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.admin.arrangor.ArrangorKontaktpersonError
import no.nav.mulighetsrommet.admin.arrangor.ArrangorKontaktpersonService
import no.nav.mulighetsrommet.admin.arrangor.KoblingerForKontaktperson
import no.nav.mulighetsrommet.api.domain.arrangor.ArrangorKontaktperson
import no.nav.mulighetsrommet.api.plugins.pathParameterUuid
import no.nav.mulighetsrommet.api.responses.StatusResponse
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.responses.respondWithStatusResponse
import no.nav.mulighetsrommet.model.FieldError
import no.nav.mulighetsrommet.model.ProblemDetail
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import no.nav.mulighetsrommet.validation.validation
import org.koin.ktor.ext.inject
import java.util.UUID

fun Route.arrangorKontaktpersonRoutes() {
    val arrangorKontaktpersoner: ArrangorKontaktpersonService by inject()

    route("arrangorer") {
        get("{id}/kontaktpersoner", {
            tags = setOf("Arrangor")
            operationId = "getArrangorKontaktpersoner"
            request {
                pathParameterUuid("id")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Kontaktpersoner lagret på arrangøren"
                    body<List<ArrangorKontaktperson>>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val id: UUID by call.parameters

            call.respond(arrangorKontaktpersoner.hentAlle(id))
        }

        put("{id}/kontaktpersoner", {
            tags = setOf("Arrangor")
            operationId = "upsertArrangorKontaktperson"
            request {
                pathParameterUuid("id")
                body<ArrangorKontaktpersonRequest>()
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Opprettet kontaktperson"
                    body<ArrangorKontaktperson>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val id: UUID by call.parameters
            val virksomhetKontaktperson = call.receive<ArrangorKontaktpersonRequest>()

            val result = virksomhetKontaktperson.toArrangorKontaktperson(id)
                .onRight { arrangorKontaktpersoner.upsert(it) }
                .onLeft { application.log.warn("Klarte ikke opprette kontaktperson: $it") }

            call.respondWithStatusResponse(result)
        }

        get("kontaktperson/{id}", {
            tags = setOf("Arrangor")
            operationId = "getKoblingerForKontaktperson"
            request {
                pathParameterUuid("id")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Koblinger for kontaktperson"
                    body<KoblingerForKontaktperson>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val id: UUID by call.parameters

            val koblinger = arrangorKontaktpersoner.hentKoblinger(id)

            call.respond(koblinger)
        }

        delete("{id}/kontaktperson/{kontaktpersonId}", {
            tags = setOf("Arrangor")
            operationId = "deleteArrangorKontaktperson"
            request {
                pathParameterUuid("id")
                pathParameterUuid("kontaktpersonId")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Kontaktpersonen ble slettet"
                }
                code(HttpStatusCode.BadRequest) {
                    description =
                        "Kontaktpersonen kunne ikke slettes fordi den fortsatt er koblet mot enten avtale eller gjennomføring"
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val id: UUID by call.parameters
            val kontaktpersonId: UUID by call.parameters

            val result = arrangorKontaktpersoner.delete(id, kontaktpersonId)
                .mapLeft {
                    val error = when (it) {
                        ArrangorKontaktpersonError.KontaktpersonErIBruk -> FieldError.of("Kontaktpersonen er i bruk og kan derfor ikke slettes")
                    }
                    ValidationError("Kunne ikke slette kontaktperson", listOf(error))
                }

            call.respondWithStatusResponse(result)
        }
    }
}

@Serializable
data class ArrangorKontaktpersonRequest(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val navn: String,
    val telefon: String?,
    val beskrivelse: String?,
    val epost: String,
    val ansvarligFor: List<ArrangorKontaktperson.Ansvar>,
) {
    fun toArrangorKontaktperson(arrangorId: UUID): StatusResponse<ArrangorKontaktperson> = validation {
        val navn = navn.trim()
        val epost = epost.trim()

        validate(navn.isNotEmpty()) {
            FieldError.of("Navn er påkrevd", ArrangorKontaktperson::navn)
        }

        validate(epost.isNotEmpty()) {
            FieldError.of("E-post er påkrevd", ArrangorKontaktperson::epost)
        }

        validate(ansvarligFor.isNotEmpty()) {
            FieldError.of("Du må velge minst ett ansvarsområde", ArrangorKontaktperson::ansvarligFor)
        }

        ArrangorKontaktperson(
            id = id,
            arrangorId = arrangorId,
            navn = navn,
            telefon = telefon?.trim()?.ifEmpty { null },
            epost = epost,
            beskrivelse = beskrivelse?.trim()?.ifEmpty { null },
            ansvarligFor = ansvarligFor,
        )
    }.mapLeft { ValidationError(errors = it) }
}
