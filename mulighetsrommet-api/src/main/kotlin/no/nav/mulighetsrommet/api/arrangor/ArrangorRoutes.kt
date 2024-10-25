package no.nav.mulighetsrommet.api.arrangor

import arrow.core.Either
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.arrangor.db.ArrangorRepository
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorKontaktperson
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorTil
import no.nav.mulighetsrommet.api.parameters.getPaginationParams
import no.nav.mulighetsrommet.api.responses.*
import no.nav.mulighetsrommet.domain.dto.Organisasjonsnummer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import org.koin.ktor.ext.inject
import java.util.*

fun Route.arrangorRoutes() {
    val arrangorRepository: ArrangorRepository by inject()
    val arrangorService: ArrangorService by inject()

    route("arrangorer") {
        post("{orgnr}") {
            val orgnr = call.parameters.getOrFail("orgnr").let { Organisasjonsnummer(it) }

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
            val pagination = getPaginationParams()
            val (totalCount, items) = arrangorRepository.getAll(
                til = filter.til,
                sok = filter.sok,
                sortering = filter.sortering,
                pagination = pagination,
            )
            call.respond(PaginatedResponse.of(pagination, totalCount, items))
        }

        get("{id}") {
            val id: UUID by call.parameters

            call.respond(arrangorRepository.getById(id))
        }

        get("hovedenhet/{id}") {
            val id: UUID by call.parameters

            call.respond(arrangorRepository.getHovedenhetBy(id))
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

        get("kontaktperson/{id}") {
            val id: UUID by call.parameters

            call.respondWithStatusResponse(arrangorService.hentKoblingerForKontaktperson(id))
        }

        delete("kontaktperson/{id}") {
            val id: UUID by call.parameters

            call.respondWithStatusResponse(arrangorService.deleteKontaktperson(id))
        }
    }
}

data class ArrangorFilter(
    val til: ArrangorTil? = null,
    val sok: String? = null,
    val sortering: String? = null,
)

fun RoutingContext.getArrangorFilter(): ArrangorFilter {
    val til = call.request.queryParameters["til"]
    val sok = call.request.queryParameters["sok"]
    val sortering = call.request.queryParameters["sortering"]
    return ArrangorFilter(
        til = til?.let { ArrangorTil.valueOf(it) },
        sok = sok,
        sortering = sortering,
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
    val ansvarligFor: List<ArrangorKontaktperson.AnsvarligFor>,
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
            if (ansvarligFor.isEmpty()) {
                add(ValidationError.of(ArrangorKontaktperson::ansvarligFor, "Du må velge minst ett ansvarsområde"))
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
                ansvarligFor = ansvarligFor,
            ),
        )
    }
}
