package no.nav.mulighetsrommet.api.routes.v1

import arrow.core.Either
import arrow.core.flatMap
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.routes.v1.responses.PaginatedResponse
import no.nav.mulighetsrommet.api.routes.v1.responses.Pagination
import no.nav.mulighetsrommet.api.services.TiltaksgjennomforingService
import no.nav.mulighetsrommet.api.utils.getAdminTiltaksgjennomforingsFilter
import no.nav.mulighetsrommet.api.utils.getPaginationParams
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import no.nav.mulighetsrommet.utils.toUUID
import org.koin.ktor.ext.inject
import java.time.LocalDate
import java.util.*

fun Route.tiltaksgjennomforingRoutes() {
    val tiltaksgjennomforingService: TiltaksgjennomforingService by inject()
    val log = application.environment.log

    route("/api/v1/internal/tiltaksgjennomforinger") {
        get {
            val paginationParams = getPaginationParams()
            val filter = getAdminTiltaksgjennomforingsFilter()
            tiltaksgjennomforingService.getAll(paginationParams, filter)
                .onRight { (totalCount, items) ->
                    call.respond(
                        PaginatedResponse(
                            pagination = Pagination(
                                totalCount = totalCount,
                                currentPage = paginationParams.page,
                                pageSize = paginationParams.limit,
                            ),
                            data = items,
                        ),
                    )
                }
                .onLeft {
                    log.error("$it")
                    call.respond(HttpStatusCode.InternalServerError, "Kunne ikke hente gjennomføringer")
                }
        }

        get("{id}") {
            val id = call.parameters["id"]?.toUUID() ?: return@get call.respondText(
                "Mangler eller ugyldig id",
                status = HttpStatusCode.BadRequest,
            )
            tiltaksgjennomforingService
                .get(id)
                .onRight {
                    if (it == null) {
                        return@get call.respondText(
                            "Det finnes ikke noe tiltaksgjennomføring med id $id",
                            status = HttpStatusCode.NotFound,
                        )
                    }
                    return@get call.respond(it)
                }
                .onLeft {
                    log.error("$it")
                    call.respond(HttpStatusCode.InternalServerError, "Kunne ikke opprette gjennomføring")
                }
        }

        put() {
            val request = call.receive<TiltaksgjennomforingRequest>()

            request.toDbo()
                .onLeft { error ->
                    call.respond(HttpStatusCode.BadRequest, error.message.toString())
                }
                .flatMap { tiltaksgjennomforingService.upsert(it) }
                .onRight { call.respond(it) }
                .onLeft { error ->
                    log.error("$error")
                    call.respond(HttpStatusCode.InternalServerError, "Kunne ikke opprette gjennomføring")
                }
        }

        get("{id}/nokkeltall") {
            val id = call.parameters["id"]?.toUUID() ?: return@get call.respondText(
                "Mangler eller ugyldig id",
                status = HttpStatusCode.BadRequest,
            )
            call.respond(tiltaksgjennomforingService.getNokkeltallForTiltaksgjennomforing(id))
        }
    }
}

@Serializable
data class TiltaksgjennomforingRequest(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val navn: String,
    @Serializable(with = UUIDSerializer::class)
    val tiltakstypeId: UUID,
    @Serializable(with = UUIDSerializer::class)
    val avtaleId: UUID,
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val sluttDato: LocalDate,
    val enhet: String? = null,
    val antallPlasser: Int,
    val virksomhetsnummer: String,
    val tiltaksnummer: String? = null,
    val ansvarlig: String,
    val enheter: List<String>,
) {
    fun toDbo(): Either<Exception, TiltaksgjennomforingDbo> {
        if (sluttDato.isBefore(startDato)) {
            return Either.Left(Exception("Sluttdato kan ikke være før startdato"))
        }
        if (antallPlasser <= 0) {
            return Either.Left(Exception("Antall plasser må være større enn 0"))
        }
        return Either.Right(
            TiltaksgjennomforingDbo(
                id = id,
                navn = navn,
                tiltakstypeId = tiltakstypeId,
                avtaleId = avtaleId,
                startDato = startDato,
                sluttDato = sluttDato,
                arenaAnsvarligEnhet = enhet,
                avslutningsstatus = Avslutningsstatus.IKKE_AVSLUTTET,
                antallPlasser = antallPlasser,
                tilgjengelighet = TiltaksgjennomforingDbo.Tilgjengelighetsstatus.Ledig,
                tiltaksnummer = tiltaksnummer,
                virksomhetsnummer = virksomhetsnummer,
                ansvarlige = listOf(ansvarlig),
                enheter = enheter,
            ),
        )
    }
}
