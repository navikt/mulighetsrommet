package no.nav.mulighetsrommet.api.routes.v1

import arrow.core.Either
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.domain.dto.ArrangorKontaktperson
import no.nav.mulighetsrommet.api.domain.dto.ArrangorTil
import no.nav.mulighetsrommet.api.repositories.ArrangorRepository
import no.nav.mulighetsrommet.api.routes.v1.responses.BadRequest
import no.nav.mulighetsrommet.api.routes.v1.responses.StatusResponse
import no.nav.mulighetsrommet.api.routes.v1.responses.ValidationError
import no.nav.mulighetsrommet.api.routes.v1.responses.respondWithStatusResponse
import no.nav.mulighetsrommet.api.services.ArrangorService
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import org.koin.ktor.ext.inject
import java.util.*

fun Route.arrangorRoutes() {
    val arrangorRepository: ArrangorRepository by inject()
    val arrangorService: ArrangorService by inject()

    route("api/v1/internal/arrangorer") {
        post("{orgnr}") {
            val orgnr = call.parameters.getOrFail("orgnr").also { validateOrgnr(it) }

            if (isUtenlandskOrgnr(orgnr)) {
                val virksomhet = arrangorRepository.get(orgnr)
                return@post if (virksomhet != null) {
                    call.respond(virksomhet)
                } else {
                    call.respond(HttpStatusCode.NotFound, "Fant ikke enhet med orgnr: $orgnr")
                }
            }

            val response = arrangorService.getOrSyncArrangorFromBrreg(orgnr)
                .mapLeft { toStatusResponseError(it) }

            call.respondWithStatusResponse(response)
        }

        get {
            val filter = getArrangorFilter()
            call.respond(arrangorRepository.getAll(til = filter.til))
        }

        get("{id}") {
            val id: UUID by call.parameters

            call.respond(arrangorRepository.getById(id))
        }

        get("{id}/kontaktpersoner") {
            val id: UUID by call.parameters

            call.respond(arrangorService.hentKontaktpersoner(id))
        }

        put("{id}/kontaktpersoner") {
            val id: UUID by call.parameters
            val virksomhetKontaktperson = call.receive<ArrangorKontaktpersonRequest>()

            val result = virksomhetKontaktperson.toDto(id)
                .map { arrangorService.upsertKontaktperson(it) }
                .onLeft { application.log.warn("Klarte ikke opprette kontaktperson: $it") }

            call.respondWithStatusResponse(result)
        }

        delete("kontaktperson/{id}") {
            val id: UUID by call.parameters

            call.respondWithStatusResponse(arrangorService.deleteKontaktperson(id))
        }
    }
}

data class ArrangorFilter(
    val til: ArrangorTil? = null,
)

fun <T : Any> PipelineContext<T, ApplicationCall>.getArrangorFilter(): ArrangorFilter {
    val til = call.request.queryParameters["til"]
    return ArrangorFilter(
        til = til?.let { ArrangorTil.valueOf(it) },
    )
}

@Serializable
data class ArrangorKontaktpersonRequest(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val navn: String,
    val telefon: String?,
    val beskrivelse: String?,
    val epost: String,
) {
    fun toDto(arrangorId: UUID): StatusResponse<ArrangorKontaktperson> {
        val navn = navn.trim()
        val epost = epost.trim()

        val errors = buildList {
            if (navn.isEmpty()) {
                add(ValidationError.of(ArrangorKontaktperson::navn, "Navn er påkrevd"))
            }
            if (epost.isEmpty()) {
                add(ValidationError.of(ArrangorKontaktperson::epost, "E-post er påkrevd"))
            }
        }

        if (errors.isNotEmpty()) {
            return Either.Left(BadRequest(errors = errors))
        }

        return Either.Right(
            ArrangorKontaktperson(
                id = id,
                arrangorId = arrangorId,
                navn = navn,
                telefon = telefon?.trim()?.ifEmpty { null },
                epost = epost,
                beskrivelse = beskrivelse?.trim()?.ifEmpty { null },
            ),
        )
    }
}
