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
import no.nav.mulighetsrommet.api.domain.dto.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.api.domain.dto.TiltaksgjennomforingKontaktpersonDbo
import no.nav.mulighetsrommet.api.routes.v1.responses.*
import no.nav.mulighetsrommet.api.services.TiltaksgjennomforingService
import no.nav.mulighetsrommet.api.services.UtkastService
import no.nav.mulighetsrommet.api.utils.getAdminTiltaksgjennomforingsFilter
import no.nav.mulighetsrommet.api.utils.getPaginationParams
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingOppstartstype
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingTilgjengelighetsstatus
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import org.koin.ktor.ext.inject
import java.time.LocalDate
import java.util.*

fun Route.tiltaksgjennomforingRoutes() {
    val tiltaksgjennomforingService: TiltaksgjennomforingService by inject()
    val utkastService: UtkastService by inject()

    route("/api/v1/internal/tiltaksgjennomforinger") {
        put {
            val request = call.receive<TiltaksgjennomforingRequest>()

            val result = request.toDbo()
                .flatMap {
                    tiltaksgjennomforingService.upsert(it)
                        .onRight { utkastService.deleteUtkast(it.id) }
                        .mapLeft { ServerError("Klarte ikke lagre tiltaksgjennomføring.") }
                }

            call.respondWithStatusResponse(result)
        }

        get {
            val paginationParams = getPaginationParams()
            val filter = getAdminTiltaksgjennomforingsFilter()

            val result = tiltaksgjennomforingService.getAll(paginationParams, filter)
                .onLeft { log.error("${it.error}") }
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

        delete("{id}") {
            val id = call.parameters.getOrFail<UUID>("id")
            call.respondWithStatusResponse(tiltaksgjennomforingService.delete(id))
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
    val arrangorOrganisasjonsnummer: String,
    @Serializable(with = UUIDSerializer::class)
    val arrangorKontaktpersonId: UUID? = null,
    val tiltaksnummer: String? = null,
    val ansvarlig: String,
    val navEnheter: List<String>,
    val oppstart: TiltaksgjennomforingOppstartstype,
    @Serializable(with = LocalDateSerializer::class)
    val stengtFra: LocalDate? = null,
    @Serializable(with = LocalDateSerializer::class)
    val stengtTil: LocalDate? = null,
    val apenForInnsok: Boolean = true,
    val kontaktpersoner: List<NavKontaktpersonForGjennomforing> = emptyList(),
    val estimertVentetid: String? = null,
    val lokasjonArrangor: String? = null,
) {
    fun toDbo(): StatusResponse<TiltaksgjennomforingDbo> {
        if (!startDato.isBefore(sluttDato)) {
            return Either.Left(BadRequest("Startdato må være før sluttdato"))
        }
        if ((stengtFra != null) != (stengtTil != null)) {
            return Either.Left(BadRequest("Både stengt fra og til må være satt"))
        }
        if (stengtFra?.isBefore(stengtTil) == false) {
            return Either.Left(BadRequest("Stengt fra må være før stengt til"))
        }
        if (antallPlasser <= 0) {
            return Either.Left(BadRequest("Antall plasser må være større enn 0"))
        }
        if (lokasjonArrangor.isNullOrEmpty()) {
            return Either.Left(BadRequest("Lokasjon for gjennomføring må være satt"))
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
                tilgjengelighet = if (apenForInnsok) {
                    TiltaksgjennomforingTilgjengelighetsstatus.LEDIG
                } else {
                    TiltaksgjennomforingTilgjengelighetsstatus.STENGT
                },
                estimertVentetid = estimertVentetid,
                tiltaksnummer = tiltaksnummer,
                arrangorOrganisasjonsnummer = arrangorOrganisasjonsnummer,
                arrangorKontaktpersonId = arrangorKontaktpersonId,
                ansvarlige = listOf(ansvarlig),
                navEnheter = navEnheter,
                oppstart = oppstart,
                opphav = ArenaMigrering.Opphav.MR_ADMIN_FLATE,
                stengtFra = stengtFra,
                stengtTil = stengtTil,
                kontaktpersoner = kontaktpersoner.map {
                    TiltaksgjennomforingKontaktpersonDbo(
                        navIdent = it.navIdent,
                        navEnheter = it.navEnheter,
                    )
                },
                lokasjonArrangor = lokasjonArrangor,
            ),
        )
    }
}

@Serializable
data class GjennomforingTilAvtaleRequest(
    @Serializable(with = UUIDSerializer::class)
    val avtaleId: UUID? = null,
)

@Serializable
data class NavKontaktpersonForGjennomforing(
    val navIdent: String,
    val navEnheter: List<String>,
)
