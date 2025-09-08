package no.nav.mulighetsrommet.api.arrangor

import arrow.core.Either
import io.github.smiley4.ktoropenapi.delete
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.github.smiley4.ktoropenapi.put
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorDto
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorKobling
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorKontaktperson
import no.nav.mulighetsrommet.api.clients.kontoregisterOrganisasjon.KontoregisterOrganisasjonClient
import no.nav.mulighetsrommet.api.parameters.getPaginationParams
import no.nav.mulighetsrommet.api.plugins.pathParameterUuid
import no.nav.mulighetsrommet.api.responses.*
import no.nav.mulighetsrommet.brreg.BrregError
import no.nav.mulighetsrommet.brreg.BrregHovedenhet
import no.nav.mulighetsrommet.brreg.BrregUnderenhet
import no.nav.mulighetsrommet.ktor.exception.BadRequest
import no.nav.mulighetsrommet.ktor.exception.InternalServerError
import no.nav.mulighetsrommet.ktor.exception.NotFound
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.ProblemDetail
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import org.koin.ktor.ext.inject
import java.util.*

fun Route.arrangorRoutes() {
    val db: ApiDatabase by inject()
    val arrangorService: ArrangorService by inject()
    val kontoregisterOrganisasjonClient: KontoregisterOrganisasjonClient by inject()

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

            if (isUtenlandskOrgnr(orgnr)) {
                val virksomhet = db.session { queries.arrangor.get(orgnr) }
                return@post if (virksomhet != null) {
                    call.respond(virksomhet)
                } else {
                    call.respond(HttpStatusCode.NotFound, "Fant ikke enhet med orgnr: $orgnr")
                }
            }

            val response = arrangorService.getArrangorOrSyncFromBrreg(orgnr)
                .mapLeft { toStatusResponseError(it, orgnr) }

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

        get("{id}/kontonummer", {
            tags = setOf("Arrangor")
            operationId = "getKontonummer"
            request {
                pathParameterUuid("id")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Kontonummer til arrangør"
                    body<ArrangorKontonummerResponse>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val orgnr = call.parameters.getOrFail("orgnr").let { Organisasjonsnummer(it) }
            val kontonummer = kontoregisterOrganisasjonClient.getKontonummerForOrganisasjon(orgnr)

            val result = kontonummer
                .mapLeft { InternalServerError("Klarte ikke hente kontonummer for arrangør") }
                .map { ArrangorKontonummerResponse(Kontonummer(it.kontonr)) }

            call.respondWithStatusResponse(result)
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
                    body<ArrangorDto>()
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

            call.respond(arrangorService.hentKontaktpersoner(id))
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

            val result = virksomhetKontaktperson.toDto(id)
                .onRight { arrangorService.upsertKontaktperson(it) }
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

            val koblinger = arrangorService.hentKoblingerForKontaktperson(id)

            call.respond(koblinger)
        }

        delete("kontaktperson/{id}", {
            tags = setOf("Arrangor")
            operationId = "deleteArrangorKontaktperson"
            request {
                pathParameterUuid("id")
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

            db.session {
                val (gjennomforinger, avtaler) = queries.arrangor.koblingerTilKontaktperson(id)

                if (gjennomforinger.isNotEmpty()) {
                    return@session call.respond(HttpStatusCode.BadRequest, "Kontaktpersonen er i bruk.")
                }

                if (avtaler.isNotEmpty()) {
                    return@session call.respond(HttpStatusCode.BadRequest, "Kontaktpersonen er i bruk.")
                }

                queries.arrangor.deleteKontaktperson(id)
            }

            call.respond(HttpStatusCode.OK)
        }
    }

    route("brreg") {
        get("sok", {
            tags = setOf("Brreg")
            operationId = "sokBrregHovedenhet"
            request {
                queryParameter<String>("q")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Treff hos Brreg"
                    body<List<BrregHovedenhet>>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val sok: String by call.request.queryParameters
            call.respondWithStatusResponse(arrangorService.brregSok(sok))
        }

        get("{orgnr}/underenheter", {
            tags = setOf("Brreg")
            operationId = "getBrregUnderenheter"
            request {
                pathParameter<String>("orgnr")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Underenhetene til hovedenhet for gitt orgnr"
                    body<List<BrregUnderenhet>>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val orgnr = call.parameters.getOrFail("orgnr").let { Organisasjonsnummer(it) }
            call.respondWithStatusResponse(arrangorService.brregUnderenheter(orgnr))
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

fun toStatusResponseError(it: BrregError, orgnr: Organisasjonsnummer) = when (it) {
    is BrregError.NotFound -> NotFound("Fant ikke bedrift $orgnr i Brreg")
    is BrregError.FjernetAvJuridiskeArsaker -> BadRequest("Bediften $orgnr er fjernet fra Brreg av juridiske årsaker")
    is BrregError.BadRequest, is BrregError.Error -> InternalServerError("Feil oppsto ved henting av bedrift $orgnr fra Brreg")
}

fun toStatusResponseError(it: BrregError) = when (it) {
    is BrregError.NotFound -> NotFound("Not Found fra Brreg")
    is BrregError.FjernetAvJuridiskeArsaker -> BadRequest("Fjernet av juridiske årsaker fra Brreg")
    is BrregError.BadRequest -> BadRequest("Bad Request mot Brreg")
    is BrregError.Error -> InternalServerError("Internal server error fra Brreg")
}

@Serializable
data class ArrangorKontonummerResponse(
    val kontonummer: Kontonummer,
)
