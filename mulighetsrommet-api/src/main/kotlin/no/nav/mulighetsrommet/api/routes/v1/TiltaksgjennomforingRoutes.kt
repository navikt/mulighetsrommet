package no.nav.mulighetsrommet.api.routes.v1

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
    val service: TiltaksgjennomforingService by inject()

    route("/api/v1/internal/tiltaksgjennomforinger") {
        put {
            val request = call.receive<TiltaksgjennomforingRequest>()
            val navIdent = getNavIdent()

            val result = service.upsert(request, navIdent)
                .mapLeft { BadRequest(errors = it) }

            call.respondWithStatusResponse(result)
        }

        get {
            val paginationParams = getPaginationParams()
            val filter = getAdminTiltaksgjennomforingsFilter()

            call.respond(service.getAllSkalMigreres(paginationParams, filter))
        }

        get("mine") {
            val paginationParams = getPaginationParams()
            val filter = getAdminTiltaksgjennomforingsFilter().copy(administratorNavIdent = getNavIdent())

            call.respond(service.getAllSkalMigreres(paginationParams, filter))
        }

        get("{id}") {
            val id = call.parameters.getOrFail<UUID>("id")

            service.get(id)
                ?.let { call.respond(it) }
                ?: call.respond(HttpStatusCode.Companion.NotFound, "Ingen tiltaksgjennomføring med id=$id")
        }

        put("{id}") {
            val id = call.parameters.getOrFail<UUID>("id")
            val request = call.receive<GjennomforingTilAvtaleRequest>()
            call.respond(service.kobleGjennomforingTilAvtale(id, request.avtaleId))
        }

        put("{id}/avbryt") {
            val id = call.parameters.getOrFail<UUID>("id")
            call.respondWithStatusResponse(service.avbrytGjennomforing(id))
        }

        get("{id}/nokkeltall") {
            val id = call.parameters.getOrFail<UUID>("id")
            call.respond(service.getNokkeltallForTiltaksgjennomforing(id))
        }

        delete("{id}") {
            val id = call.parameters.getOrFail<UUID>("id")
            call.respondWithStatusResponse(service.delete(id))
        }

        put("{id}/tilgjengeligForVeileder") {
            val id = call.parameters.getOrFail<UUID>("id")
            val request = call.receive<TilgjengeligForVeilederRequest>()
            service.setTilgjengeligForVeileder(id, request.tilgjengeligForVeileder)
            call.respond(HttpStatusCode.OK)
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
    fun toDbo() = TiltaksgjennomforingDbo(
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
    )
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

@Serializable
data class TilgjengeligForVeilederRequest(
    val tilgjengeligForVeileder: Boolean,
)
