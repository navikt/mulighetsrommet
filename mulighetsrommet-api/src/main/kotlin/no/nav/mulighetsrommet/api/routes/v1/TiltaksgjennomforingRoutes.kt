package no.nav.mulighetsrommet.api.routes.v1

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.routes.v1.responses.*
import no.nav.mulighetsrommet.api.services.TiltaksgjennomforingService
import no.nav.mulighetsrommet.api.utils.getAdminTiltaksgjennomforingsFilter
import no.nav.mulighetsrommet.api.utils.getPaginationParams
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import org.koin.ktor.ext.inject
import java.time.LocalDate
import java.util.*

fun Route.tiltaksgjennomforingRoutes() {
    val tiltaksgjennomforingService: TiltaksgjennomforingService by inject()

    route("/api/v1/internal/tiltaksgjennomforinger") {
        put {
            val request = call.receive<TiltaksgjennomforingRequest>()

            val result = request.toDbo()
                .flatMap {
                    tiltaksgjennomforingService.upsert(it)
                        .mapLeft { ServerError("Klarte ikke lagre tiltaksgjennomføring") }
                }

            call.respondWithStatusResponse(result)
        }

        get {
            val paginationParams = getPaginationParams()
            val filter = getAdminTiltaksgjennomforingsFilter()

            val result = tiltaksgjennomforingService.getAll(paginationParams, filter)
                .mapLeft { ServerError("Klarte ikke hente tiltaksgjennomføringer") }

            call.respondWithStatusResponse(result)
        }

        get("{id}") {
            val id = call.parameters.getOrFail<UUID>("id")

            val result = tiltaksgjennomforingService.get(id)
                .flatMap { it?.right() ?: NotFound("Ingen tiltaksgjennomføring med id=$id").left() }
                .mapLeft { ServerError("Klarte ikke hente tiltaksgjennomføring med id=$id") }

            call.respondWithStatusResponse(result)
        }

        put("{id}") {
            val gjennomforingId = call.parameters.getOrFail<UUID>("id")
            val request = call.receive<GjennomforingTilAvtaleRequest>()
            call.respond(tiltaksgjennomforingService.kobleGjennomforingTilAvtale(gjennomforingId, request.avtaleId))
        }

        get("{id}/nokkeltall") {
            val id = call.parameters.getOrFail<UUID>("id")
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
    val navEnheter: List<String>,
    val oppstart: TiltaksgjennomforingDbo.Oppstartstype,
) {
    fun toDbo(): StatusResponse<TiltaksgjennomforingDbo> {
        if (sluttDato.isBefore(startDato)) {
            return Either.Left(BadRequest("Sluttdato kan ikke være før startdato"))
        }
        if (antallPlasser <= 0) {
            return Either.Left(BadRequest("Antall plasser må være større enn 0"))
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
                navEnheter = navEnheter,
                oppstart = oppstart,
            ),
        )
    }
}

@Serializable
data class GjennomforingTilAvtaleRequest(
    @Serializable(with = UUIDSerializer::class)
    val avtaleId: UUID? = null,
)
