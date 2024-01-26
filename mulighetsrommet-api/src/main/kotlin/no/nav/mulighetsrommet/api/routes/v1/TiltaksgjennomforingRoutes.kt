package no.nav.mulighetsrommet.api.routes.v1

import arrow.core.Either
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.AppConfig
import no.nav.mulighetsrommet.api.domain.dbo.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.api.domain.dbo.TiltaksgjennomforingKontaktpersonDbo
import no.nav.mulighetsrommet.api.plugins.AuthProvider
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.repositories.DeltakerRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.api.routes.v1.responses.BadRequest
import no.nav.mulighetsrommet.api.routes.v1.responses.ValidationError
import no.nav.mulighetsrommet.api.routes.v1.responses.respondWithStatusResponse
import no.nav.mulighetsrommet.api.services.TiltaksgjennomforingService
import no.nav.mulighetsrommet.api.utils.getAdminTiltaksgjennomforingsFilter
import no.nav.mulighetsrommet.api.utils.getPaginationParams
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingOppstartstype
import no.nav.mulighetsrommet.domain.dto.Faneinnhold
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import org.koin.ktor.ext.inject
import java.time.LocalDate
import java.util.*

fun Route.tiltaksgjennomforingRoutes(appConfig: AppConfig) {
    val deltakere: DeltakerRepository by inject()
    val service: TiltaksgjennomforingService by inject()
    val tiltakstyper: TiltakstypeRepository by inject()
    val tiltaksgjennomforinger: TiltaksgjennomforingRepository by inject()

    route("/api/v1/internal/tiltaksgjennomforinger") {
        authenticate(
            AuthProvider.AZURE_AD_TILTAKSJENNOMFORINGER_SKRIV.name,
            strategy = AuthenticationStrategy.Required,
        ) {
            put {
                val request = call.receive<TiltaksgjennomforingRequest>()
                val navIdent = getNavIdent()

                // TODO Fjern tiltakstypesjekk når vi har blitt master for alle tiltakstyper
                val tiltakstype = tiltakstyper.get(request.tiltakstypeId)
                    ?: throw BadRequestException("Fant ikke tiltakstype med id: ${request.tiltakstypeId}")

                val tiltaksgjennomforingEksisterer = tiltaksgjennomforinger.get(request.id)

                if (tiltaksgjennomforingEksisterer == null && !appConfig.kafka.producers.arenaMigreringTiltaksgjennomforinger.tiltakstyper.contains(tiltakstype.arenaKode)) {
                    call.respondWithStatusResponse(
                        Either.Left(
                            BadRequest(
                                errors = listOf(
                                    ValidationError(
                                        name = "avtale",
                                        message = "Opprettelse av tiltaksgjennomføring for tiltakstype: '${tiltakstype.navn}' er ikke skrudd på enda.",
                                    ),
                                ),
                            ),
                        ),
                    )
                }

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
                val request = call.receive<TilgjengeligForVeilederRequest>()
                service.setTilgjengeligForVeileder(id, request.tilgjengeligForVeileder, navIdent)
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

@Serializable
data class TiltaksgjennomforingDeltakerSummary(
    val antallDeltakere: Int,
)

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
    val administratorer: List<String>,
    val navRegion: String,
    val navEnheter: List<String>,
    val oppstart: TiltaksgjennomforingOppstartstype,
    @Serializable(with = LocalDateSerializer::class)
    val stengtFra: LocalDate?,
    @Serializable(with = LocalDateSerializer::class)
    val stengtTil: LocalDate?,
    val apentForInnsok: Boolean,
    val kontaktpersoner: List<NavKontaktpersonForGjennomforing>,
    val stedForGjennomforing: String?,
    val opphav: ArenaMigrering.Opphav?,
    val faneinnhold: Faneinnhold?,
    val beskrivelse: String?,
    val deltidsprosent: Double,
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
        tiltaksnummer = tiltaksnummer,
        arrangorOrganisasjonsnummer = arrangorOrganisasjonsnummer,
        arrangorKontaktpersonId = arrangorKontaktpersonId,
        administratorer = administratorer,
        navRegion = navRegion,
        navEnheter = navEnheter,
        oppstart = oppstart,
        opphav = opphav ?: ArenaMigrering.Opphav.MR_ADMIN_FLATE,
        stengtFra = stengtFra,
        stengtTil = stengtTil,
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
    )
}

@Serializable
data class SetAvtaleForGjennomforingRequest(
    @Serializable(with = UUIDSerializer::class)
    val avtaleId: UUID? = null,
)

@Serializable
data class NavKontaktpersonForGjennomforing(
    val navIdent: String,
    val navEnheter: List<String>,
    val beskrivelse: String?,
)

@Serializable
data class TilgjengeligForVeilederRequest(
    val tilgjengeligForVeileder: Boolean,
)
