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
import kotlinx.serialization.json.JsonObject
import no.nav.mulighetsrommet.api.domain.dbo.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.api.domain.dbo.TiltaksgjennomforingKontaktpersonDbo
import no.nav.mulighetsrommet.api.domain.dto.FrikobleKontaktpersonRequest
import no.nav.mulighetsrommet.api.plugins.AuthProvider
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.repositories.DeltakerRepository
import no.nav.mulighetsrommet.api.routes.v1.parameters.getPaginationParams
import no.nav.mulighetsrommet.api.routes.v1.responses.BadRequest
import no.nav.mulighetsrommet.api.routes.v1.responses.respondWithStatusResponse
import no.nav.mulighetsrommet.api.services.TiltaksgjennomforingService
import no.nav.mulighetsrommet.domain.dbo.Deltakerstatus
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingOppstartstype
import no.nav.mulighetsrommet.domain.dto.AmoKategorisering
import no.nav.mulighetsrommet.domain.dto.AvbruttAarsak
import no.nav.mulighetsrommet.domain.dto.Faneinnhold
import no.nav.mulighetsrommet.domain.dto.NavIdent
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingStatus
import no.nav.mulighetsrommet.domain.serializers.AvbruttAarsakSerializer
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import org.koin.ktor.ext.inject
import java.time.LocalDate
import java.util.*

fun Route.tiltaksgjennomforingRoutes() {
    val deltakere: DeltakerRepository by inject()
    val service: TiltaksgjennomforingService by inject()

    route("/api/v1/intern/tiltaksgjennomforinger") {
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
                val request = call.receive<AvbrytRequest>()
                val response = service.avbrytGjennomforing(id, navIdent, request.aarsak)
                call.respondWithStatusResponse(response)
            }

            put("{id}/tilgjengelig-for-veileder") {
                val id = call.parameters.getOrFail<UUID>("id")
                val navIdent = getNavIdent()
                val request = call.receive<PublisertRequest>()
                service.setPublisert(id, request.publisert, navIdent)
                call.respond(HttpStatusCode.OK)
            }

            delete("kontaktperson") {
                val request = call.receive<FrikobleKontaktpersonRequest>()
                val navIdent = getNavIdent()
                call.respondWithStatusResponse(
                    service.frikobleKontaktpersonFraGjennomforing(
                        kontaktpersonId = request.kontaktpersonId,
                        gjennomforingId = request.dokumentId,
                        navIdent = navIdent,
                    ),
                )
            }
        }

        get {
            val pagination = getPaginationParams()
            val filter = getAdminTiltaksgjennomforingsFilter()

            call.respond(service.getAllSkalMigreres(pagination, filter))
        }

        get("mine") {
            val pagination = getPaginationParams()
            val filter = getAdminTiltaksgjennomforingsFilter().copy(administratorNavIdent = getNavIdent())

            call.respond(service.getAllSkalMigreres(pagination, filter))
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
            val groupedDeltakere = deltakereForGjennomforing.groupBy { it.status }
            val summary = TiltaksgjennomforingDeltakerSummary(
                antallDeltakere = deltakereForGjennomforing.size,
                antallAktiveDeltakere = groupedDeltakere.getOrDefault(Deltakerstatus.DELTAR, emptyList()).size,
                antallDeltakereSomVenter = groupedDeltakere.getOrDefault(Deltakerstatus.VENTER, emptyList()).size,
                antallAvsluttedeDeltakere = groupedDeltakere.getOrDefault(Deltakerstatus.AVSLUTTET, emptyList()).size,
                antallIkkeAktuelleDeltakere = groupedDeltakere.getOrDefault(
                    Deltakerstatus.IKKE_AKTUELL,
                    emptyList(),
                ).size,
                pabegyntRegistrering = groupedDeltakere.getOrDefault(
                    Deltakerstatus.PABEGYNT_REGISTRERING,
                    emptyList(),
                ).size,
            )

            call.respond(summary)
        }
    }
}

data class AdminTiltaksgjennomforingFilter(
    val search: String? = null,
    val navEnheter: List<String> = emptyList(),
    val tiltakstypeIder: List<UUID> = emptyList(),
    val statuser: List<TiltaksgjennomforingStatus.Enum> = emptyList(),
    val sortering: String? = null,
    val avtaleId: UUID? = null,
    val arrangorIds: List<UUID> = emptyList(),
    val administratorNavIdent: NavIdent? = null,
    val publisert: Boolean? = null,
)

fun <T : Any> PipelineContext<T, ApplicationCall>.getAdminTiltaksgjennomforingsFilter(): AdminTiltaksgjennomforingFilter {
    val search = call.request.queryParameters["search"]
    val navEnheter = call.parameters.getAll("navEnheter") ?: emptyList()
    val tiltakstypeIder = call.parameters.getAll("tiltakstyper")?.map { UUID.fromString(it) } ?: emptyList()
    val statuser = call.parameters.getAll("statuser")
        ?.map { TiltaksgjennomforingStatus.Enum.valueOf(it) }
        ?: emptyList()
    val sortering = call.request.queryParameters["sort"]
    val avtaleId = call.request.queryParameters["avtaleId"]?.let { if (it.isEmpty()) null else UUID.fromString(it) }
    val arrangorIds = call.parameters.getAll("arrangorer")?.map { UUID.fromString(it) } ?: emptyList()
    val publisert = call.request.queryParameters["publisert"]?.let { it.toBoolean() }

    return AdminTiltaksgjennomforingFilter(
        search = search,
        navEnheter = navEnheter,
        tiltakstypeIder = tiltakstypeIder,
        statuser = statuser,
        sortering = sortering,
        avtaleId = avtaleId,
        arrangorIds = arrangorIds,
        administratorNavIdent = null,
        publisert = publisert,
    )
}

@Serializable
data class TiltaksgjennomforingDeltakerSummary(
    val antallDeltakere: Int,
    val antallAktiveDeltakere: Int,
    val antallDeltakereSomVenter: Int,
    val antallAvsluttedeDeltakere: Int,
    val antallIkkeAktuelleDeltakere: Int,
    val pabegyntRegistrering: Int,
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
    @Serializable(with = LocalDateSerializer::class)
    val tilgjengeligForArrangorFraOgMedDato: LocalDate?,
    val nusData: JsonObject?,
    val amoKategorisering: AmoKategorisering?,
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
        tilgjengeligForArrangorFraOgMedDato = tilgjengeligForArrangorFraOgMedDato,
        nusData = nusData,
        amoKategorisering = amoKategorisering,
    )
}

@Serializable
data class AvbrytRequest(
    @Serializable(with = AvbruttAarsakSerializer::class)
    val aarsak: AvbruttAarsak?,
)

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
