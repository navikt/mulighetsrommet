package no.nav.mulighetsrommet.api.arrangor

import arrow.core.Either
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.arrangor.db.DokumentKoblingForKontaktperson
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorKontaktperson
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorTil
import no.nav.mulighetsrommet.api.clients.kontoregisterOrganisasjon.KontoregisterOrganisasjonClient
import no.nav.mulighetsrommet.api.parameters.getPaginationParams
import no.nav.mulighetsrommet.api.responses.*
import no.nav.mulighetsrommet.brreg.BrregError
import no.nav.mulighetsrommet.ktor.exception.BadRequest
import no.nav.mulighetsrommet.ktor.exception.InternalServerError
import no.nav.mulighetsrommet.ktor.exception.NotFound
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import org.koin.ktor.ext.inject
import java.util.*

fun Route.arrangorRoutes() {
    val db: ApiDatabase by inject()
    val arrangorService: ArrangorService by inject()
    val kontoregisterOrganisasjonClient: KontoregisterOrganisasjonClient by inject()

    route("arrangorer") {
        post("{orgnr}") {
            val orgnr = call.parameters.getOrFail("orgnr").let { Organisasjonsnummer(it) }

            if (isUtenlandskOrgnr(orgnr)) {
                val virksomhet = db.session { queries.arrangor.get(orgnr) }
                return@post if (virksomhet != null) {
                    call.respond(virksomhet)
                } else {
                    call.respond(HttpStatusCode.NotFound, "Fant ikke enhet med orgnr: $orgnr")
                }
            }

            val response = arrangorService.getArrangorOrSyncFromBrreg(orgnr)
                .mapLeft { toStatusResponseError(it) }

            call.respondWithStatusResponse(response)
        }

        get("{orgnr}/kontonummer") {
            val orgnr = call.parameters.getOrFail("orgnr").let { Organisasjonsnummer(it) }
            val kontonummer = kontoregisterOrganisasjonClient.getKontonummerForOrganisasjon(orgnr)

            kontonummer.map { call.respond(it.kontonr) }.mapLeft {
                call.respond(HttpStatusCode.InternalServerError, "Klarte ikke hente kontonummer for arrangør")
            }
        }

        get {
            val filter = getArrangorFilter()
            val pagination = getPaginationParams()

            val (totalCount, items) = db.session {
                queries.arrangor.getAll(
                    til = filter.til,
                    sok = filter.sok,
                    sortering = filter.sortering,
                    pagination = pagination,
                )
            }

            call.respond(PaginatedResponse.of(pagination, totalCount, items))
        }

        get("{id}") {
            val id: UUID by call.parameters

            val arrangor = db.session { queries.arrangor.getById(id) }

            call.respond(arrangor)
        }

        get("hovedenhet/{id}") {
            val id: UUID by call.parameters

            val arrangor = db.session { queries.arrangor.getHovedenhetById(id) }

            call.respond(arrangor)
        }

        get("{id}/kontaktpersoner") {
            val id: UUID by call.parameters

            call.respond(arrangorService.hentKontaktpersoner(id))
        }

        put("{id}/kontaktpersoner") {
            val id: UUID by call.parameters
            val virksomhetKontaktperson = call.receive<ArrangorKontaktpersonRequest>()

            val result = virksomhetKontaktperson.toDto(id)
                .onRight { arrangorService.upsertKontaktperson(it) }
                .onLeft { application.log.warn("Klarte ikke opprette kontaktperson: $it") }

            call.respondWithStatusResponse(result)
        }

        get("kontaktperson/{id}") {
            val id: UUID by call.parameters

            val koblinger = arrangorService.hentKoblingerForKontaktperson(id)

            call.respond(koblinger)
        }

        delete("kontaktperson/{id}") {
            val id: UUID by call.parameters

            db.session {
                val (gjennomforinger, avtaler) = queries.arrangor.koblingerTilKontaktperson(id)

                if (gjennomforinger.isNotEmpty<DokumentKoblingForKontaktperson>()) {
                    return@session call.respond(HttpStatusCode.BadRequest, "Kontaktpersonen er i bruk.")
                }

                if (avtaler.isNotEmpty<DokumentKoblingForKontaktperson>()) {
                    return@session call.respond(HttpStatusCode.BadRequest, "Kontaktpersonen er i bruk.")
                }

                queries.arrangor.deleteKontaktperson(id)
            }

            call.respond(HttpStatusCode.OK)
        }
    }

    route("brreg") {
        get("sok") {
            val sok: String by call.request.queryParameters
            call.respondWithStatusResponse(arrangorService.brregSok(sok))
        }

        get("{orgnr}/underenheter") {
            val orgnr = call.parameters.getOrFail("orgnr").let { Organisasjonsnummer(it) }
            call.respondWithStatusResponse(arrangorService.brregUnderenheter(orgnr))
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
                add(FieldError.of(ArrangorKontaktperson::navn, "Navn er påkrevd"))
            }
            if (epost.isEmpty()) {
                add(FieldError.of(ArrangorKontaktperson::epost, "E-post er påkrevd"))
            }
            if (ansvarligFor.isEmpty()) {
                add(FieldError.of(ArrangorKontaktperson::ansvarligFor, "Du må velge minst ett ansvarsområde"))
            }
        }

        if (errors.isNotEmpty()) {
            return Either.Left(ValidationError(errors = errors))
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

fun toStatusResponseError(it: BrregError) = when (it) {
    BrregError.NotFound -> NotFound("not found fra brreg")
    BrregError.BadRequest -> BadRequest("bad request mot brreg")
    BrregError.Error -> InternalServerError("brreg internal server error")
}
