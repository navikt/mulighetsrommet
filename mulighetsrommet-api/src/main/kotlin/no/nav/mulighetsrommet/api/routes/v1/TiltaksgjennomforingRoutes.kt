package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.domain.dbo.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.api.domain.dbo.TiltaksgjennomforingKontaktpersonDbo
import no.nav.mulighetsrommet.api.plugins.AuthProvider
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.repositories.DeltakerRepository
import no.nav.mulighetsrommet.api.routes.v1.responses.BadRequest
import no.nav.mulighetsrommet.api.routes.v1.responses.respondWithStatusResponse
import no.nav.mulighetsrommet.api.services.TiltaksgjennomforingService
import no.nav.mulighetsrommet.api.utils.AdminTiltaksgjennomforingFilter
import no.nav.mulighetsrommet.api.utils.getPaginationParams
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingOppstartstype
import no.nav.mulighetsrommet.domain.dto.Faneinnhold
import no.nav.mulighetsrommet.domain.dto.NavIdent
import no.nav.mulighetsrommet.domain.dto.Tiltaksgjennomforingsstatus
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import org.koin.ktor.ext.inject
import java.time.LocalDate
import java.util.*

fun Route.tiltaksgjennomforingRoutes() {
    val deltakere: DeltakerRepository by inject()
    val service: TiltaksgjennomforingService by inject()

    route("/api/v1/internal/tiltaksgjennomforinger") {
        authenticate(
            AuthProvider.AZURE_AD_TILTAKSJENNOMFORINGER_SKRIV.name,
            strategy = AuthenticationStrategy.Required,
        ) {
            put {
                val request = call.receive<TiltaksgjennomforingRequest>()
                val navIdent = getNavIdent()

                val result = service.upsert(request, navIdent)
                    .mapLeft { BadRequest(errors = it) }

                call.respondWithStatusResponse(result)
            }

            put("{id}/avtale") {
                val id = call.parameters.getOrFail<UUID>("id")
                val navIdent = getNavIdent()
                val request = call.receive<SetAvtaleForGjennomforingRequest>()
                val response = service.setAvtale(id, request.avtaleId, navIdent)
                call.respondWithStatusResponse(response)
            }

            put("{id}/avbryt") {
                val id = call.parameters.getOrFail<UUID>("id")
                val navIdent = getNavIdent()
                val response = service.avbrytGjennomforing(id, navIdent)
                call.respondWithStatusResponse(response)
            }

            put("{id}/tilgjengelig-for-veileder") {
                val id = call.parameters.getOrFail<UUID>("id")
                val navIdent = getNavIdent()
                val request = call.receive<PublisertRequest>()
                service.setPublisert(id, request.publisert, navIdent)
                call.respond(HttpStatusCode.OK)
            }
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
                ?: call.respond(HttpStatusCode.NotFound, "Ingen tiltaksgjennomføring med id=$id")
        }

        get("{id}/tiltaksnummer") {
            val id = call.parameters.getOrFail<UUID>("id")

            service.get(id)
                ?.let { gjennomforing ->
                    gjennomforing.tiltaksnummer
                        ?.let { call.respond(TiltaksnummerResponse(tiltaksnummer = it)) }
                        ?: call.respond(HttpStatusCode.NoContent)
                }
                ?: call.respond(HttpStatusCode.NotFound, "Ingen tiltaksgjennomføring med id=$id")
        }

        get("{id}/historikk") {
            val id: UUID by call.parameters
            val historikk = service.getEndringshistorikk(id)
            call.respond(historikk)
        }

        get("{id}/deltaker-summary") {
            val id: UUID by call.parameters

            val deltakereForGjennomforing = deltakere.getAll(id)
            val summary = TiltaksgjennomforingDeltakerSummary(antallDeltakere = deltakereForGjennomforing.size)

            call.respond(summary)
        }
    }
}

fun <T : Any> PipelineContext<T, ApplicationCall>.getAdminTiltaksgjennomforingsFilter(): AdminTiltaksgjennomforingFilter {
    val search = call.request.queryParameters["search"]
    val navEnheter = call.parameters.getAll("navEnheter") ?: emptyList()
    val tiltakstypeIder = call.parameters.getAll("tiltakstyper")?.map { UUID.fromString(it) } ?: emptyList()
    val statuser = call.parameters.getAll("statuser")
        ?.map { Tiltaksgjennomforingsstatus.valueOf(it) }
        ?: emptyList()
    val sortering = call.request.queryParameters["sort"]
    val avtaleId = call.request.queryParameters["avtaleId"]?.let { if (it.isEmpty()) null else UUID.fromString(it) }
    val arrangorIds = call.parameters.getAll("arrangorer")?.map { UUID.fromString(it) } ?: emptyList()

    return AdminTiltaksgjennomforingFilter(
        search = search,
        navEnheter = navEnheter,
        tiltakstypeIder = tiltakstypeIder,
        statuser = statuser,
        sortering = sortering,
        avtaleId = avtaleId,
        arrangorIds = arrangorIds,
        administratorNavIdent = null,
    )
}

@Serializable
data class TiltaksgjennomforingDeltakerSummary(
    val antallDeltakere: Int,
)

@Serializable
data class TiltaksnummerResponse(
    val tiltaksnummer: String,
)

@Serializable
data class TiltaksgjennomforingRequest(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = UUIDSerializer::class)
    val tiltakstypeId: UUID,
    @Serializable(with = UUIDSerializer::class)
    val avtaleId: UUID,
    val navn: String,
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val sluttDato: LocalDate?,
    val antallPlasser: Int,
    @Serializable(with = UUIDSerializer::class)
    val arrangorId: UUID,
    val arrangorKontaktpersoner: List<
        @Serializable(with = UUIDSerializer::class)
        UUID,
        >,
    val administratorer: List<NavIdent>,
    val navRegion: String,
    val navEnheter: List<String>,
    val oppstart: TiltaksgjennomforingOppstartstype,
    val apentForInnsok: Boolean,
    val kontaktpersoner: List<NavKontaktpersonForGjennomforing>,
    val stedForGjennomforing: String?,
    val faneinnhold: Faneinnhold?,
    val beskrivelse: String?,
    val deltidsprosent: Double,
    val estimertVentetid: EstimertVentetid?,
) {
    fun toDbo() = TiltaksgjennomforingDbo(
        id = id,
        navn = navn,
        tiltakstypeId = tiltakstypeId,
        avtaleId = avtaleId,
        startDato = startDato,
        sluttDato = sluttDato,
        antallPlasser = antallPlasser,
        apentForInnsok = apentForInnsok,
        arrangorId = arrangorId,
        arrangorKontaktpersoner = arrangorKontaktpersoner,
        administratorer = administratorer,
        navRegion = navRegion,
        navEnheter = navEnheter,
        oppstart = oppstart,
        kontaktpersoner = kontaktpersoner.map {
            TiltaksgjennomforingKontaktpersonDbo(
                navIdent = it.navIdent,
                navEnheter = it.navEnheter,
                beskrivelse = it.beskrivelse,
            )
        },
        stedForGjennomforing = stedForGjennomforing,
        faneinnhold = faneinnhold,
        beskrivelse = beskrivelse,
        deltidsprosent = deltidsprosent,
        estimertVentetidVerdi = estimertVentetid?.verdi,
        estimertVentetidEnhet = estimertVentetid?.enhet,
    )
}

@Serializable
data class SetAvtaleForGjennomforingRequest(
    @Serializable(with = UUIDSerializer::class)
    val avtaleId: UUID? = null,
)

@Serializable
data class NavKontaktpersonForGjennomforing(
    val navIdent: NavIdent,
    val navEnheter: List<String>,
    val beskrivelse: String?,
)

@Serializable
data class PublisertRequest(
    val publisert: Boolean,
)

@Serializable
data class EstimertVentetid(
    val verdi: Int,
    val enhet: String,
)
