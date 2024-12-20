package no.nav.mulighetsrommet.api.gjennomforing

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.avtale.db.AvtaleRepository
import no.nav.mulighetsrommet.api.domain.dto.FrikobleKontaktpersonRequest
import no.nav.mulighetsrommet.api.gjennomforing.db.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.api.gjennomforing.db.TiltaksgjennomforingKontaktpersonDbo
import no.nav.mulighetsrommet.api.parameters.getPaginationParams
import no.nav.mulighetsrommet.api.plugins.AuthProvider
import no.nav.mulighetsrommet.api.plugins.authenticate
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.refusjon.db.DeltakerRepository
import no.nav.mulighetsrommet.api.responses.BadRequest
import no.nav.mulighetsrommet.api.responses.ServerError
import no.nav.mulighetsrommet.api.responses.respondWithStatusResponse
import no.nav.mulighetsrommet.api.responses.respondWithStatusResponseError
import no.nav.mulighetsrommet.api.services.EndretAv
import no.nav.mulighetsrommet.api.services.ExcelService
import no.nav.mulighetsrommet.domain.Tiltakskoder.isForhaandsgodkjentTiltak
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingOppstartstype
import no.nav.mulighetsrommet.domain.dto.*
import no.nav.mulighetsrommet.domain.serializers.AvbruttAarsakSerializer
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import no.nav.mulighetsrommet.utdanning.db.UtdanningslopDbo
import org.koin.ktor.ext.inject
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

fun Route.tiltaksgjennomforingRoutes() {
    val deltakere: DeltakerRepository by inject()
    val service: TiltaksgjennomforingService by inject()
    val avtaler: AvtaleRepository by inject()

    route("tiltaksgjennomforinger") {
        authenticate(AuthProvider.AZURE_AD_TILTAKSJENNOMFORINGER_SKRIV) {
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

                val gjennomforing = service.get(id) ?: return@put call.respond(
                    HttpStatusCode.NotFound,
                    message = "Gjennomføringen finnes ikke",
                )

                if (!isForhaandsgodkjentTiltak(gjennomforing.tiltakstype.tiltakskode)) {
                    return@put call.respond(
                        HttpStatusCode.BadRequest,
                        message = "Avtale kan bare settes for tiltaksgjennomføringer av type AFT eller VTA",
                    )
                }

                val avtaleId = request.avtaleId?.also {
                    val avtale = avtaler.get(it) ?: return@put call.respond(
                        HttpStatusCode.BadRequest,
                        message = "Avtale med id=$it finnes ikke",
                    )
                    if (gjennomforing.tiltakstype.id != avtale.tiltakstype.id) {
                        return@put call.respond(
                            HttpStatusCode.BadRequest,
                            message = "Tiltaksgjennomføringen må ha samme tiltakstype som avtalen",
                        )
                    }
                }

                service.setAvtale(gjennomforing.id, avtaleId, navIdent)

                call.respond(HttpStatusCode.OK)
            }

            put("{id}/avbryt") {
                val id = call.parameters.getOrFail<UUID>("id")
                val navIdent = getNavIdent()
                val request = call.receive<AvbrytRequest>()

                val gjennomforing = service.get(id) ?: return@put call.respond(
                    HttpStatusCode.NotFound,
                    message = "Gjennomføringen finnes ikke",
                )

                if (gjennomforing.status.status != TiltaksgjennomforingStatus.GJENNOMFORES) {
                    return@put call.respond(
                        HttpStatusCode.BadRequest,
                        message = "Gjennomføringen er allerede avsluttet og kan derfor ikke avbrytes.",
                    )
                }

                val aarsak = request.aarsak
                    ?: return@put call.respond(HttpStatusCode.BadRequest, message = "Årsak mangler")

                if (aarsak is AvbruttAarsak.Annet && aarsak.beskrivelse.length > 100) {
                    return@put call.respond(
                        HttpStatusCode.BadRequest,
                        message = "Beskrivelse kan ikke inneholde mer enn 100 tegn",
                    )
                }

                if (aarsak is AvbruttAarsak.Annet && aarsak.beskrivelse.isEmpty()) {
                    return@put call.respond(
                        HttpStatusCode.BadRequest,
                        message = "Beskrivelse er obligatorisk når “Annet” er valgt som årsak",
                    )
                }

                service.setAvsluttet(id, LocalDateTime.now(), aarsak, EndretAv.NavAnsatt(navIdent))

                call.respond(HttpStatusCode.OK)
            }

            put("{id}/tilgjengelig-for-veileder") {
                val id = call.parameters.getOrFail<UUID>("id")
                val navIdent = getNavIdent()
                val request = call.receive<PublisertRequest>()
                service.setPublisert(id, request.publisert, navIdent)
                call.respond(HttpStatusCode.OK)
            }

            put("{id}/apent-for-pamelding") {
                val id = call.parameters.getOrFail<UUID>("id")
                val navIdent = getNavIdent()
                val request = call.receive<SetApentForPameldingRequest>()
                service.setApentForPamelding(id, request.apentForPamelding, EndretAv.NavAnsatt(navIdent))
                call.respond(HttpStatusCode.OK)
            }

            put("{id}/tilgjengelig-for-arrangor") {
                val id = call.parameters.getOrFail<UUID>("id")
                val request = call.receive<SetTilgjengligForArrangorRequest>()
                val navIdent = getNavIdent()

                val response = service
                    .setTilgjengeligForArrangorDato(
                        id,
                        request.tilgjengeligForArrangorDato,
                        navIdent,
                    )
                    .mapLeft { BadRequest(errors = it) }

                call.respondWithStatusResponse(response)
            }

            delete("kontaktperson") {
                val request = call.receive<FrikobleKontaktpersonRequest>()
                val navIdent = getNavIdent()
                try {
                    service.frikobleKontaktpersonFraGjennomforing(
                        kontaktpersonId = request.kontaktpersonId,
                        gjennomforingId = request.dokumentId,
                        navIdent = navIdent,
                    )
                } catch (e: Throwable) {
                    application.environment.log.error(
                        "Klarte ikke fjerne kontaktperson fra gjennomføring: $request",
                        e,
                    )
                    call.respondWithStatusResponseError(
                        ServerError("Klarte ikke fjerne kontaktperson fra gjennomføringen"),
                    )
                }
            }
        }

        get {
            val pagination = getPaginationParams()
            val filter = getAdminTiltaksgjennomforingsFilter()

            call.respond(service.getAll(pagination, filter))
        }

        get("mine") {
            val pagination = getPaginationParams()
            val filter = getAdminTiltaksgjennomforingsFilter().copy(administratorNavIdent = getNavIdent())

            call.respond(service.getAll(pagination, filter))
        }

        get("/excel") {
            val pagination = getPaginationParams()
            val filter = getAdminTiltaksgjennomforingsFilter()
            val navIdent = call.parameters["visMineTiltaksgjennomforinger"]?.let {
                if (it == "true") {
                    getNavIdent()
                } else {
                    null
                }
            }
            val overstyrtFilter = filter.copy(
                administratorNavIdent = navIdent,
            )

            val result = service.getAll(pagination, overstyrtFilter)
            val file = ExcelService.createExcelFileForTiltaksgjennomforing(result.data)
            call.response.header(
                HttpHeaders.ContentDisposition,
                ContentDisposition.Attachment
                    .withParameter(ContentDisposition.Parameters.FileName, "tiltaksgjennomforinger.xlsx")
                    .toString(),
            )
            call.response.header("Access-Control-Expose-Headers", HttpHeaders.ContentDisposition)
            call.response.header(
                HttpHeaders.ContentType,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            )
            call.respondFile(file)
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

            val summary = TiltaksgjennomforingDeltakerSummary(
                antallDeltakere = deltakereForGjennomforing.size,
                deltakereByStatus = deltakereForGjennomforing
                    .groupBy { it.status.type }
                    .map { (status, deltakere) ->
                        DeltakerStatusSummary(status = status.description, count = deltakere.size)
                    },
            )

            call.respond(summary)
        }
    }
}

data class AdminTiltaksgjennomforingFilter(
    val search: String? = null,
    val navEnheter: List<String> = emptyList(),
    val tiltakstypeIder: List<UUID> = emptyList(),
    val statuser: List<TiltaksgjennomforingStatus> = emptyList(),
    val sortering: String? = null,
    val avtaleId: UUID? = null,
    val arrangorIds: List<UUID> = emptyList(),
    val administratorNavIdent: NavIdent? = null,
    val publisert: Boolean? = null,
)

fun RoutingContext.getAdminTiltaksgjennomforingsFilter(): AdminTiltaksgjennomforingFilter {
    val search = call.request.queryParameters["search"]
    val navEnheter = call.parameters.getAll("navEnheter") ?: emptyList()
    val tiltakstypeIder = call.parameters.getAll("tiltakstyper")?.map { UUID.fromString(it) } ?: emptyList()
    val statuser = call.parameters.getAll("statuser")
        ?.map { TiltaksgjennomforingStatus.valueOf(it) }
        ?: emptyList()
    val sortering = call.request.queryParameters["sort"]
    val avtaleId = call.request.queryParameters["avtaleId"]?.let { if (it.isEmpty()) null else UUID.fromString(it) }
    val arrangorIds = call.parameters.getAll("arrangorer")?.map { UUID.fromString(it) } ?: emptyList()
    val publisert = call.request.queryParameters["publisert"]?.toBoolean()

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
    val deltakereByStatus: List<DeltakerStatusSummary>,
)

@Serializable
data class DeltakerStatusSummary(
    val status: String,
    val count: Int,
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
    val kontaktpersoner: List<TiltaksgjennomforingKontaktpersonDto>,
    val stedForGjennomforing: String?,
    val faneinnhold: Faneinnhold?,
    val beskrivelse: String?,
    val deltidsprosent: Double,
    val estimertVentetid: EstimertVentetid?,
    @Serializable(with = LocalDateSerializer::class)
    val tilgjengeligForArrangorFraOgMedDato: LocalDate?,
    val amoKategorisering: AmoKategorisering?,
    val utdanningslop: UtdanningslopDbo? = null,
) {
    fun toDbo() = TiltaksgjennomforingDbo(
        id = id,
        navn = navn,
        tiltakstypeId = tiltakstypeId,
        avtaleId = avtaleId,
        startDato = startDato,
        sluttDato = sluttDato,
        antallPlasser = antallPlasser,
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
        amoKategorisering = amoKategorisering,
        utdanningslop = utdanningslop,
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
data class TiltaksgjennomforingKontaktpersonDto(
    val navIdent: NavIdent,
    val navEnheter: List<String>,
    val beskrivelse: String?,
)

@Serializable
data class PublisertRequest(
    val publisert: Boolean,
)

@Serializable
data class SetApentForPameldingRequest(
    val apentForPamelding: Boolean,
)

@Serializable
data class SetTilgjengligForArrangorRequest(
    @Serializable(with = LocalDateSerializer::class)
    val tilgjengeligForArrangorDato: LocalDate,
)

@Serializable
data class EstimertVentetid(
    val verdi: Int,
    val enhet: String,
)
