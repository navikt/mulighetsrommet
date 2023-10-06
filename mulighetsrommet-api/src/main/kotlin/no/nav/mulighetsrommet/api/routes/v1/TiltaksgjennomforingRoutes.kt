package no.nav.mulighetsrommet.api.routes.v1

import arrow.core.Either
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.domain.dbo.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.api.domain.dbo.TiltaksgjennomforingKontaktpersonDbo
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.routes.v1.responses.BadRequest
import no.nav.mulighetsrommet.api.routes.v1.responses.StatusResponse
import no.nav.mulighetsrommet.api.routes.v1.responses.respondWithStatusResponse
import no.nav.mulighetsrommet.api.services.TiltaksgjennomforingService
import no.nav.mulighetsrommet.api.utils.getAdminTiltaksgjennomforingsFilter
import no.nav.mulighetsrommet.api.utils.getPaginationParams
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingOppstartstype
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingTilgjengelighetsstatus
import no.nav.mulighetsrommet.domain.dto.Faneinnhold
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
            val navIdent = getNavIdent()

            call.respondWithStatusResponse(tiltaksgjennomforingService.upsert(request, navIdent))
        }

        get {
            val paginationParams = getPaginationParams()
            val filter = getAdminTiltaksgjennomforingsFilter()

            call.respond(tiltaksgjennomforingService.getAllSkalMigreres(paginationParams, filter))
        }

        get("mine") {
            val paginationParams = getPaginationParams()
            val filter = getAdminTiltaksgjennomforingsFilter().copy(administratorNavIdent = getNavIdent())

            call.respond(tiltaksgjennomforingService.getAllSkalMigreres(paginationParams, filter))
        }

        get("{id}") {
            val id = call.parameters.getOrFail<UUID>("id")

            tiltaksgjennomforingService.get(id)
                ?.let { call.respond(it) }
                ?: call.respond(HttpStatusCode.Companion.NotFound, "Ingen tiltaksgjennomføring med id=$id")
        }

        put("{id}") {
            val gjennomforingId = call.parameters.getOrFail<UUID>("id")
            val request = call.receive<GjennomforingTilAvtaleRequest>()
            call.respond(tiltaksgjennomforingService.kobleGjennomforingTilAvtale(gjennomforingId, request.avtaleId))
        }

        put("{id}/avbryt") {
            val gjennomforingId = call.parameters.getOrFail<UUID>("id")
            call.respondWithStatusResponse(tiltaksgjennomforingService.avbrytGjennomforing(gjennomforingId))
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
    val sluttDato: LocalDate?,
    val antallPlasser: Int,
    val arrangorOrganisasjonsnummer: String,
    @Serializable(with = UUIDSerializer::class)
    val arrangorKontaktpersonId: UUID?,
    val tiltaksnummer: String?,
    val administrator: String,
    val navEnheter: List<String>,
    val oppstart: TiltaksgjennomforingOppstartstype,
    @Serializable(with = LocalDateSerializer::class)
    val stengtFra: LocalDate?,
    @Serializable(with = LocalDateSerializer::class)
    val stengtTil: LocalDate?,
    val apenForInnsok: Boolean,
    val kontaktpersoner: List<NavKontaktpersonForGjennomforing>,
    val estimertVentetid: String?,
    val stedForGjennomforing: String,
    val opphav: ArenaMigrering.Opphav?,
    val faneinnhold: Faneinnhold?,
    val beskrivelse: String?,
) {
    fun toDbo(): StatusResponse<TiltaksgjennomforingDbo> {
        if (sluttDato != null && !startDato.isBefore(sluttDato)) {
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
        if (navEnheter.isEmpty()) {
            return Either.Left(BadRequest("Navenheter kan ikke være tom"))
        }

        return Either.Right(
            TiltaksgjennomforingDbo(
                id = id,
                navn = navn,
                tiltakstypeId = tiltakstypeId,
                avtaleId = avtaleId,
                startDato = startDato,
                sluttDato = sluttDato,
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
                administratorer = listOf(administrator),
                navEnheter = navEnheter,
                oppstart = oppstart,
                opphav = opphav ?: ArenaMigrering.Opphav.MR_ADMIN_FLATE,
                stengtFra = stengtFra,
                stengtTil = stengtTil,
                kontaktpersoner = kontaktpersoner.map {
                    TiltaksgjennomforingKontaktpersonDbo(
                        navIdent = it.navIdent,
                        navEnheter = it.navEnheter,
                    )
                },
                stedForGjennomforing = stedForGjennomforing,
                faneinnhold = faneinnhold,
                beskrivelse = beskrivelse,
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
