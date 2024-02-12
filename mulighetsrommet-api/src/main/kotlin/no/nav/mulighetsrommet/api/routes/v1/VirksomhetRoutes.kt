package no.nav.mulighetsrommet.api.routes.v1

import arrow.core.Either
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.clients.brreg.BrregClient
import no.nav.mulighetsrommet.api.clients.brreg.BrregError
import no.nav.mulighetsrommet.api.clients.brreg.OrgnummerUtil
import no.nav.mulighetsrommet.api.domain.dto.VirksomhetKontaktperson
import no.nav.mulighetsrommet.api.repositories.VirksomhetRepository
import no.nav.mulighetsrommet.api.routes.v1.responses.*
import no.nav.mulighetsrommet.api.services.VirksomhetService
import no.nav.mulighetsrommet.api.utils.getVirksomhetFilter
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import org.koin.ktor.ext.inject
import java.util.*

fun Route.virksomhetRoutes() {
    val virksomhetRepository: VirksomhetRepository by inject()
    val virksomhetService: VirksomhetService by inject()
    val brregClient: BrregClient by inject()

    route("api/v1/internal/virksomhet") {
        get {
            val filter = getVirksomhetFilter()
            call.respond(virksomhetRepository.getAll(til = filter.til))
        }

        get("{orgnr}") {
            val orgnr = call.parameters.getOrFail("orgnr")

            if (!OrgnummerUtil.erOrgnr(orgnr)) {
                throw BadRequestException("Verdi sendt inn er ikke et organisasjonsnummer. Organisasjonsnummer er 9 siffer og bare tall.")
            }

            val enhet = virksomhetService.getOrSyncVirksomhet(orgnr)
            if (enhet == null) {
                call.respond(HttpStatusCode.NoContent, "Fant ikke enhet med orgnr: $orgnr")
            } else {
                call.respond(enhet)
            }
        }

        get("{orgnr}/kontaktperson") {
            val orgnr = call.parameters.getOrFail("orgnr")

            if (!OrgnummerUtil.erOrgnr(orgnr)) {
                throw BadRequestException("Verdi sendt inn er ikke et organisasjonsnummer. Organisasjonsnummer er 9 siffer og bare tall.")
            }

            call.respond(virksomhetService.hentKontaktpersoner(orgnr))
        }

        put("{orgnr}/kontaktperson") {
            val orgnr = call.parameters.getOrFail("orgnr")

            if (!OrgnummerUtil.erOrgnr(orgnr)) {
                throw BadRequestException("Verdi sendt inn er ikke et organisasjonsnummer. Organisasjonsnummer er 9 siffer og bare tall.")
            }

            val virksomhetKontaktperson = call.receive<VirksomhetKontaktpersonRequest>()
            val result = virksomhetKontaktperson
                .toDto(orgnr)
                .map { virksomhetService.upsertKontaktperson(it) }
                .onLeft {
                    application.log.error(it.message)
                }
            call.respondWithStatusResponse(result)
        }

        delete("kontaktperson/{id}") {
            val id = call.parameters.getOrFail<UUID>("id")

            call.respondWithStatusResponse(virksomhetService.deleteKontaktperson(id))
        }

        get("sok/{sok}") {
            val sokestreng = call.parameters.getOrFail("sok")

            if (sokestreng.isBlank()) {
                throw BadRequestException("'sok' kan ikke være en tom streng")
            }

            val response = brregClient.sokEtterOverordnetEnheter(sokestreng)
                .mapLeft {
                    when (it) {
                        BrregError.NotFound -> NotFound()
                        BrregError.BadRequest -> BadRequest()
                        BrregError.Error -> ServerError()
                    }
                }

            call.respondWithStatusResponse(response)
        }

        post("/update") {
            val orgnr = call.request.queryParameters.getOrFail("orgnr")

            if (orgnr.length != 9) {
                throw BadRequestException("'orgnr' må inneholde 9 siffer")
            }

            virksomhetService.syncVirksomhetFraBrreg(orgnr)
                .onRight { virksomhet ->
                    call.respond("${virksomhet.navn} oppdatert")
                }
                .onLeft { error ->
                    if (error == BrregError.Error) {
                        call.respond(HttpStatusCode.InternalServerError)
                    } else {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            "Klarte ikke synkronisere virksomhet med orgnr=$orgnr. Er orgnr riktig?",
                        )
                    }
                }
        }
    }
}

@Serializable
data class VirksomhetKontaktpersonRequest(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val navn: String,
    val telefon: String?,
    val beskrivelse: String?,
    val epost: String,
) {
    fun toDto(orgnr: String): StatusResponse<VirksomhetKontaktperson> {
        if (navn.isEmpty() || epost.isEmpty()) {
            return Either.Left(BadRequest("Verdier kan ikke være tomme"))
        }

        return Either.Right(
            VirksomhetKontaktperson(
                id = id,
                organisasjonsnummer = orgnr,
                navn = navn,
                telefon = telefon,
                epost = epost,
                beskrivelse = beskrivelse,
            ),
        )
    }
}
