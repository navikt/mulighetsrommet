package no.nav.mulighetsrommet.api.arrangorflate.api

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.nel
import arrow.core.raise.either
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.route
import io.ktor.server.util.getOrFail
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import no.nav.mulighetsrommet.altinn.AltinnRettigheterService
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.OkonomiConfig
import no.nav.mulighetsrommet.api.arrangorflate.ArrangorflateService
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorInnsendingRadDto
import no.nav.mulighetsrommet.api.arrangorflate.dto.toRadDto
import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
import no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingGruppetiltak
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.api.utbetaling.UtbetalingInputHelper.resolveAvtaltPrisPerTimeOppfolgingPerDeltaker
import no.nav.mulighetsrommet.api.utbetaling.UtbetalingService
import no.nav.mulighetsrommet.api.utbetaling.UtbetalingValidator
import no.nav.mulighetsrommet.api.utbetaling.UtbetalingValidator.maksUtbetalingsPeriodeSluttDato
import no.nav.mulighetsrommet.api.utbetaling.UtbetalingValidator.minAntallVedleggVedOpprettKrav
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakelsePeriode
import no.nav.mulighetsrommet.api.utbetaling.model.Deltaker
import no.nav.mulighetsrommet.api.utbetaling.model.SatsPeriode
import no.nav.mulighetsrommet.api.utbetaling.model.StengtPeriode
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.arena.ArenaMigrering
import no.nav.mulighetsrommet.clamav.ClamAvClient
import no.nav.mulighetsrommet.clamav.Status
import no.nav.mulighetsrommet.clamav.Vedlegg
import no.nav.mulighetsrommet.ktor.exception.BadRequest
import no.nav.mulighetsrommet.ktor.exception.InternalServerError
import no.nav.mulighetsrommet.ktor.exception.StatusException
import no.nav.mulighetsrommet.ktor.plugins.respondWithProblemDetail
import no.nav.mulighetsrommet.model.Arrangor
import no.nav.mulighetsrommet.model.DataDetails
import no.nav.mulighetsrommet.model.DataDrivenTableDto
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.LabeledDataElement
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.ProblemDetail
import no.nav.mulighetsrommet.model.TiltakstypeStatus
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import org.koin.ktor.ext.inject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.collections.listOf

fun Route.arrangorflateRoutesOpprettKrav(okonomiConfig: OkonomiConfig) {
    val db: ApiDatabase by inject()
    val utbetalingService: UtbetalingService by inject()
    val arrangorFlateService: ArrangorflateService by inject()
    val clamAvClient: ClamAvClient by inject()
    val altinnRettigheterService: AltinnRettigheterService by inject()

    fun requireGjennomforingTilArrangor(
        gjennomforing: Gjennomforing,
        organisasjonsnummer: Organisasjonsnummer,
    ) = if (gjennomforing.arrangor.organisasjonsnummer != organisasjonsnummer) {
        throw StatusException(HttpStatusCode.Forbidden, "Ikke gjennomføring til bedrift")
    } else {
        Unit
    }

    suspend fun RoutingContext.requireGjennomforing(): GjennomforingGruppetiltak {
        val orgnr = call.parameters.getOrFail("orgnr").let { Organisasjonsnummer(it) }
        requireTilgangHosArrangor(altinnRettigheterService, orgnr)

        val gjennomforingId = call.parameters.getOrFail("gjennomforingId").let { UUID.fromString(it) }
        val gjennomforing = db.session { queries.gjennomforing.getGruppetiltakOrError(gjennomforingId) }
        requireGjennomforingTilArrangor(gjennomforing, orgnr)

        return gjennomforing
    }

    fun RoutingContext.getPeriodeFromQuery(): Periode {
        val periodeStart = call.queryParameters["periodeStart"]?.let { start ->
            LocalDate.parse(start, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        }
        val periodeSlutt = call.queryParameters["periodeSlutt"]?.let { start ->
            LocalDate.parse(start, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        }
        return periodeStart?.let { start -> periodeSlutt?.let { slutt -> Periode.of(start, slutt) } }
            ?: throw StatusException(HttpStatusCode.BadRequest, "Periode er ikke oppgitt")
    }

    get("/tiltaksoversikt", {
        description = "Hent tiltakene for alle arrangører brukeren har tilgang til"
        tags = setOf("Arrangorflate")
        operationId = "getArrangorTiltaksoversikt"
        request {
            queryParameter<TiltaksoversiktType>("type")
        }
        response {
            code(HttpStatusCode.OK) {
                description = "Tiltak for arrangør"
                body<List<ArrangorInnsendingRadDto>>()
            }
            default {
                description = "Problem details"
                body<ProblemDetail>()
            }
        }
    }) {
        val arrangorer = orgnrTilganger(altinnRettigheterService)
        if (arrangorer.isEmpty()) {
            respondWithManglerTilgangHosArrangor()
            return@get
        }
        val type = TiltaksoversiktType.from(call.queryParameters["type"])
        val gjennomforinger = db.session {
            val gyldigeTiltakstyper = queries.tiltakstype
                .getAll(statuser = listOf(TiltakstypeStatus.AKTIV))
                .map { it.id }

            val gyldigePrismodeller = okonomiConfig.opprettKravPrismodeller

            if (gyldigePrismodeller.isEmpty() || gyldigeTiltakstyper.isEmpty()) {
                return@session emptyList()
            } else {
                queries.gjennomforing
                    .getAllGruppetiltakKompakt(
                        arrangorOrgnr = arrangorer,
                        prismodeller = gyldigePrismodeller,
                        tiltakstypeIder = gyldigeTiltakstyper,
                        sluttDatoGreaterThanOrEqualTo = ArenaMigrering.TiltaksgjennomforingSluttDatoCutoffDate,
                        statuser = type.toGjennomforingStatuses(),
                    )
                    .items
            }
        }

        call.respond(
            gjennomforinger.map { it.toRadDto() },
        )
    }

    route("/arrangor/{orgnr}/gjennomforing/{gjennomforingId}/opprett-krav") {
        get({
            description = "Hent veiviser informasjon"
            tags = setOf("Arrangorflate")
            operationId = "getOpprettKravVeiviser"
            request {
                pathParameter<Organisasjonsnummer>("orgnr")
                pathParameter<String>("gjennomforingId")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Veiviser metadata (steg)"
                    body<OpprettKravVeiviserMeta>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val gjennomforing = requireGjennomforing()

            val stegListe = getVeiviserSteg(gjennomforing)
            call.respond(OpprettKravVeiviserMeta(stegListe.map { it.toDto() }))
        }

        get("/innsendingsinformasjon", {
            description = "Hent innsendingsinformasjon"
            tags = setOf("Arrangorflate")
            operationId = "getOpprettKravInnsendingsinformasjon"
            request {
                pathParameter<Organisasjonsnummer>("orgnr")
                pathParameter<String>("gjennomforingId")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Innsendingsdetaljer"
                    body<OpprettKravInnsendingsInformasjon>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val gjennomforing = requireGjennomforing()

            val tilsagnstyper =
                if (gjennomforing.prismodell?.type == PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK) {
                    listOf(TilsagnType.INVESTERING)
                } else {
                    listOf(TilsagnType.TILSAGN, TilsagnType.EKSTRATILSAGN)
                }
            val tilsagn = arrangorFlateService.getTilsagn(
                arrangorer = setOf(gjennomforing.arrangor.organisasjonsnummer),
                typer = tilsagnstyper,
                statuser = listOf(TilsagnStatus.GODKJENT),
                gjennomforingId = gjennomforing.id,
            )

            // TODO: Inkluder filtrering på eksisternde utbetalinger
            // val tidligereUtbetalingsPerioder = db.session { queries.utbetaling.getByGjennomforing(gjennomforing.id) }.map { it.periode }.toSet()
            val payload = OpprettKravInnsendingsInformasjon.from(
                okonomiConfig,
                gjennomforing,
                tilsagn,
                tidligereUtbetalinger = emptyList(),
            )
            call.respond(payload)
        }

        get("/deltakere", {
            description = "Hent deltakertabell"
            tags = setOf("Arrangorflate")
            operationId = "getOpprettKravDeltakere"
            request {
                pathParameter<Organisasjonsnummer>("orgnr")
                pathParameter<String>("gjennomforingId")
                queryParameter<String>("periodeStart")
                queryParameter<String>("periodeSlutt")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Innsendingsdetaljer"
                    body<OpprettKravDeltakere>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val gjennomforing = requireGjennomforing()

            val periode = getPeriodeFromQuery()

            val avtaltPrisPerTimeOppfolgingPerDeltaker = db.session {
                resolveAvtaltPrisPerTimeOppfolgingPerDeltaker(gjennomforing, periode)
            }

            val personalia = arrangorFlateService.getPersonalia(
                avtaltPrisPerTimeOppfolgingPerDeltaker
                    .deltakelsePerioder.map { it.deltakelseId }.toSet(),
            )
            call.respond(
                OpprettKravDeltakere.from(
                    gjennomforing,
                    satser = avtaltPrisPerTimeOppfolgingPerDeltaker.satser,
                    stengtHosArrangor = avtaltPrisPerTimeOppfolgingPerDeltaker.stengtHosArrangor,
                    deltakere = avtaltPrisPerTimeOppfolgingPerDeltaker.deltakere,
                    deltakelsePerioder = avtaltPrisPerTimeOppfolgingPerDeltaker.deltakelsePerioder
                        .sortedBy { it.periode.start },
                    personalia,
                ),
            )
        }

        get("/utbetalingsinformasjon", {
            description = "Hent utbetalingsinformasjon"
            tags = setOf("Arrangorflate")
            operationId = "getOpprettKravUtbetalingsinformasjon"
            request {
                pathParameter<Organisasjonsnummer>("orgnr")
                pathParameter<String>("gjennomforingId")
                queryParameter<String>("periodeStart")
                queryParameter<String>("periodeSlutt")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Utbetalingsinformasjon"
                    body<OpprettKravUtbetalingsinformasjon>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val gjennomforing = requireGjennomforing()
            arrangorFlateService.getKontonummer(gjennomforing.arrangor.organisasjonsnummer)
                .onLeft { call.respondWithProblemDetail(InternalServerError("Klarte ikke å hente kontonummeret")) }
                .onRight { kontonummer ->
                    call.respond(
                        OpprettKravUtbetalingsinformasjon.from(
                            gjennomforing,
                            kontonummer,
                        ),
                    )
                }
        }

        get("/vedlegg", {
            description = "Hent vedleggsinfo"
            tags = setOf("Arrangorflate")
            operationId = "getOpprettKravVedlegg"
            request {
                pathParameter<Organisasjonsnummer>("orgnr")
                pathParameter<String>("gjennomforingId")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Vedleggsinfo"
                    body<OpprettKravVedlegg>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val gjennomforing = requireGjennomforing()
            call.respond(
                OpprettKravVedlegg.from(gjennomforing),
            )
        }

        post("/oppsummering", {
            description = "Hent oppsummering"
            tags = setOf("Arrangorflate")
            operationId = "getOpprettKravOppsummering"
            request {
                pathParameter<Organisasjonsnummer>("orgnr")
                pathParameter<String>("gjennomforingId")
                body<OpprettKravOppsummeringRequest> {
                    description = "Request for creating a payment claim"
                }
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Informasjon om opprettet krav om utbetaling"
                    body<OpprettKravOppsummering>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val gjennomforing = requireGjennomforing()
            val request = call.receive<OpprettKravOppsummeringRequest>()
            arrangorFlateService.getKontonummer(gjennomforing.arrangor.organisasjonsnummer)
                .onLeft { call.respondWithProblemDetail(InternalServerError("Klarte ikke å hente kontonummeret")) }
                .onRight { kontonummer ->
                    call.respond(
                        OpprettKravOppsummering.from(request, gjennomforing, kontonummer),
                    )
                }
        }

        post({
            description = "Opprett krav om utbetaling"
            tags = setOf("Arrangorflate")
            operationId = "postOpprettKrav"
            request {
                pathParameter<Organisasjonsnummer>("orgnr")
                pathParameter<String>("gjennomforingId")
                body<OpprettKravUtbetalingRequest> {
                    description = "Request for creating a payment claim"
                    mediaTypes(ContentType.MultiPart.FormData)
                }
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Informasjon om opprettet krav om utbetaling"
                    body<OpprettKravUtbetalingResponse>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val gjennomforing = requireGjennomforing()
            receiveOpprettKravUtbetalingRequest()
                .onLeft { return@post call.respondWithProblemDetail(ValidationError(errors = it)) }
                .flatMap { request ->
                    // Scan vedlegg for virus
                    if (clamAvClient.virusScanVedlegg(request.vedlegg).any { it.Result == Status.FOUND }) {
                        return@post call.respondWithProblemDetail(BadRequest("Virus funnet i minst ett vedlegg"))
                    }

                    arrangorFlateService.getKontonummer(gjennomforing.arrangor.organisasjonsnummer)
                        .mapLeft { FieldError("/kontonummer", "Klarte ikke hente kontonummer").nel() }
                        .flatMap { kontonummer ->
                            UtbetalingValidator.validateOpprettKravArrangorflate(
                                request,
                                gjennomforing,
                                okonomiConfig,
                                kontonummer,
                            )
                        }
                        .flatMap { utbetalingService.opprettUtbetaling(it, gjennomforing, Arrangor) }
                        .onLeft { errors ->
                            call.respondWithProblemDetail(ValidationError("Klarte ikke opprette utbetaling", errors))
                        }
                        .onRight { utbetaling -> call.respond(OpprettKravOmUtbetalingResponse(utbetaling.id)) }
                }
        }
    }
}

@Serializable
enum class TiltaksoversiktType {
    AKTIVE,
    HISTORISKE,
    ;

    fun toGjennomforingStatuses(): List<GjennomforingStatusType> = when (this) {
        AKTIVE -> listOf(GjennomforingStatusType.GJENNOMFORES)
        HISTORISKE -> GjennomforingStatusType.entries.filter { it != GjennomforingStatusType.GJENNOMFORES }
    }

    companion object {
        /**
         * Defaulter til AKTIVE
         */

        fun from(type: String?): TiltaksoversiktType = when (type) {
            "AKTIVE" -> AKTIVE
            "HISTORISKE" -> HISTORISKE
            else -> AKTIVE
        }
    }
}

@Serializable
data class OpprettKravVeiviserMeta(val steg: List<OpprettKravVeiviserStegDto>)

@Serializable
enum class OpprettKravVeiviserSteg(val navn: String, val order: Int) {
    INFORMASJON("Innsendingsinformasjon", 1),
    DELTAKERLISTE("Deltakere", 2),
    UTBETALING("Utbetalingsinformasjon", 3),
    VEDLEGG("Vedlegg", 4),
    OPPSUMMERING("Oppsummering", 5),
}

fun getVeiviserSteg(gjennomforing: GjennomforingGruppetiltak): List<OpprettKravVeiviserSteg> {
    val stegListe = mutableListOf(
        OpprettKravVeiviserSteg.INFORMASJON,
        OpprettKravVeiviserSteg.UTBETALING,
        OpprettKravVeiviserSteg.VEDLEGG,
        OpprettKravVeiviserSteg.OPPSUMMERING,
    )

    if (gjennomforing.prismodell?.type == PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER) {
        stegListe.add(OpprettKravVeiviserSteg.DELTAKERLISTE)
    }
    stegListe.sortBy { it.order }
    return stegListe
}

fun OpprettKravVeiviserSteg.toDto(): OpprettKravVeiviserStegDto = OpprettKravVeiviserStegDto(type = this, navn = navn, order = order)

@Serializable
data class OpprettKravVeiviserStegDto(val type: OpprettKravVeiviserSteg, val navn: String, val order: Int)

@Serializable
data class OpprettKravVeiviserNavigering(val tilbake: OpprettKravVeiviserSteg?, val neste: OpprettKravVeiviserSteg?)

fun getVeiviserNavigering(
    steg: OpprettKravVeiviserSteg,
    gjennomforing: GjennomforingGruppetiltak,
): OpprettKravVeiviserNavigering {
    val stegListe = getVeiviserSteg(gjennomforing)
    val stegIndex = stegListe.indexOf(steg)
    return OpprettKravVeiviserNavigering(
        tilbake = stegListe.getOrNull(stegIndex - 1),
        neste = stegListe.getOrNull(stegIndex + 1),
    )
}

@Serializable
data class OpprettKravInnsendingsInformasjon(
    val guidePanel: GuidePanelType?,
    val definisjonsListe: List<LabeledDataElement>,
    val tilsagn: List<ArrangorflateTilsagnDto>,
    val datoVelger: DatoVelger,
    val navigering: OpprettKravVeiviserNavigering,
) {
    companion object {
        fun from(
            okonomiConfig: OkonomiConfig,
            gjennomforing: GjennomforingGruppetiltak,
            tilsagn: List<ArrangorflateTilsagnDto>,
            tidligereUtbetalinger: List<Utbetaling>,
        ): OpprettKravInnsendingsInformasjon {
            val navigering = getVeiviserNavigering(OpprettKravVeiviserSteg.INFORMASJON, gjennomforing)
            val datoVelger = DatoVelger.from(
                okonomiConfig,
                gjennomforing,
                tidligereUtbetalingsPerioder = tidligereUtbetalinger.map { it.periode }.toSet(),
            )

            return OpprettKravInnsendingsInformasjon(
                guidePanel = panelGuide(gjennomforing.prismodell?.type),
                definisjonsListe = getInnsendingsInformasjon(gjennomforing),
                tilsagn = tilsagn,
                datoVelger = datoVelger,
                navigering = navigering,
            )
        }

        fun panelGuide(prismodell: PrismodellType?): GuidePanelType? = when (prismodell) {
            PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK ->
                GuidePanelType.INVESTERING_VTA_AFT

            PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER ->
                GuidePanelType.TIMESPRIS

            PrismodellType.ANNEN_AVTALT_PRIS ->
                GuidePanelType.AVTALT_PRIS

            else -> null
        }
    }
}

fun getInnsendingsInformasjon(gjennomforing: GjennomforingGruppetiltak): List<LabeledDataElement> {
    val standardList = listOf(
        LabeledDataElement.text(
            "Arrangør",
            "${gjennomforing.arrangor.navn} - ${gjennomforing.arrangor.organisasjonsnummer.value}",
        ),
        LabeledDataElement.text("Tiltaksnavn", gjennomforing.navn),
        LabeledDataElement.text("Tiltakstype", gjennomforing.tiltakstype.navn),
    )
    if (gjennomforing.prismodell?.type == PrismodellType.ANNEN_AVTALT_PRIS) {
        return standardList +
            LabeledDataElement.text(
                "Tiltaksperiode",
                Periode.formatPeriode(gjennomforing.startDato, gjennomforing.sluttDato!!),
            )
    }
    return standardList
}

@Serializable
enum class GuidePanelType {
    INVESTERING_VTA_AFT,
    TIMESPRIS,
    AVTALT_PRIS,
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed class DatoVelger {
    @Serializable
    @SerialName("DatoVelgerSelect")
    data class DatoSelect(val periodeForslag: List<Periode>) : DatoVelger()

    @Serializable
    @SerialName("DatoVelgerRange")
    data class DatoRange(
        @Serializable(with = LocalDateSerializer::class)
        val maksSluttdato: LocalDate,
    ) : DatoVelger()

    companion object {
        fun from(
            okonomiConfig: OkonomiConfig,
            gjennomforing: GjennomforingGruppetiltak,
            tidligereUtbetalingsPerioder: Set<Periode> = emptySet(),
        ): DatoVelger {
            when (gjennomforing.prismodell?.type) {
                PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER -> {
                    // Har de nådd innsendingssteget, kan vi garantere at tiltakskoden er konfigurert opp
                    val tilsagnPeriode =
                        okonomiConfig.gyldigTilsagnPeriode[gjennomforing.tiltakstype.tiltakskode]!!

                    val firstOfThisMonth = LocalDate.now().withDayOfMonth(1)
                    val perioder = Periode(
                        start = maxOf(tilsagnPeriode.start, gjennomforing.startDato),
                        slutt = minOf(firstOfThisMonth, gjennomforing.sluttDato ?: firstOfThisMonth),
                    ).splitByMonth()
                    val filtrertePerioder =
                        perioder.filter { it !in tidligereUtbetalingsPerioder }.sortedBy { it.start }
                    return DatoSelect(filtrertePerioder)
                }

                PrismodellType.ANNEN_AVTALT_PRIS,
                PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK,
                -> {
                    return DatoRange(
                        maksUtbetalingsPeriodeSluttDato(
                            gjennomforing,
                            okonomiConfig,
                        ),
                    )
                }

                else ->
                    throw StatusException(
                        HttpStatusCode.Forbidden,
                        "Du kan ikke opprette utbetalingskrav for denne tiltaksgjennomføringen",
                    )
            }
        }
    }
}

@Serializable
data class OpprettKravVedlegg(
    val guidePanel: GuidePanelType?,
    val minAntallVedlegg: Int,
    val navigering: OpprettKravVeiviserNavigering,
) {

    companion object {
        fun from(gjennomforing: GjennomforingGruppetiltak): OpprettKravVedlegg {
            return OpprettKravVedlegg(
                guidePanel = GuidePanelType.from(gjennomforing.prismodell?.type),
                minAntallVedlegg = minAntallVedleggVedOpprettKrav(gjennomforing.prismodell?.type),
                navigering = getVeiviserNavigering(OpprettKravVeiviserSteg.VEDLEGG, gjennomforing),
            )
        }
    }

    @Serializable
    enum class GuidePanelType {
        INVESTERING_VTA_AFT,
        TIMESPRIS,
        AVTALT_PRIS,
        ;

        companion object {
            fun from(prismodellType: PrismodellType?): GuidePanelType? = when (prismodellType) {
                PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK -> INVESTERING_VTA_AFT
                PrismodellType.ANNEN_AVTALT_PRIS -> AVTALT_PRIS
                PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER -> TIMESPRIS
                else -> null
            }
        }
    }
}

@Serializable
data class OpprettKravDeltakere(
    val guidePanel: GuidePanelType,
    val stengtHosArrangor: Set<StengtPeriode>,
    val tabell: DataDrivenTableDto,
    val tabellFooter: List<DataDetails>,
    val navigering: OpprettKravVeiviserNavigering,
) {
    companion object {
        fun from(
            gjennomforing: GjennomforingGruppetiltak,
            satser: Set<SatsPeriode>,
            stengtHosArrangor: Set<StengtPeriode>,
            deltakere: List<Deltaker>,
            deltakelsePerioder: List<DeltakelsePeriode>,
            personalia: Map<UUID, ArrangorflatePersonalia>,
        ): OpprettKravDeltakere {
            requireNotNull(gjennomforing.prismodell?.type)
            return OpprettKravDeltakere(
                guidePanel = GuidePanelType.from(gjennomforing.prismodell.type),
                stengtHosArrangor = stengtHosArrangor,
                tabell = DataDrivenTableDto(
                    columns = deltakelseCommonColumns(),
                    rows = deltakelsePerioder.map { deltakelsePeriode ->
                        DataDrivenTableDto.Row(
                            cells = deltakelseCommonCells(
                                personalia[deltakelsePeriode.deltakelseId],
                                deltakere.find { it.id == deltakelsePeriode.deltakelseId }?.startDato,
                                deltakelsePeriode.periode,
                            ),
                        )
                    },
                ),
                tabellFooter = tableFooter(satser, deltakelsePerioder.size),
                navigering = getVeiviserNavigering(OpprettKravVeiviserSteg.DELTAKERLISTE, gjennomforing),
            )
        }

        fun tableFooter(
            satser: Set<SatsPeriode>,
            antallDeltakere: Int,
        ): List<DataDetails> {
            return listOf(
                DataDetails(
                    entries = listOf(
                        LabeledDataElement.number(
                            "Antall deltakere",
                            antallDeltakere,
                        ),
                    ),
                ),
            ) +
                beregningSatsPeriodeDetaljerUtenFaktor(satser.toList(), "Avtalt pris per time oppfølging")
        }
    }

    @Serializable
    enum class GuidePanelType {
        GENERELL,
        TIMESPRIS,
        ;

        companion object {
            fun from(prismodellType: PrismodellType?): GuidePanelType = when (prismodellType) {
                PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER -> TIMESPRIS

                PrismodellType.ANNEN_AVTALT_PRIS,
                PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK,
                PrismodellType.AVTALT_PRIS_PER_MANEDSVERK,
                PrismodellType.AVTALT_PRIS_PER_UKESVERK,
                PrismodellType.AVTALT_PRIS_PER_HELE_UKESVERK,
                null,
                -> GENERELL
            }
        }
    }
}

@Serializable
data class OpprettKravUtbetalingsinformasjon(
    val kontonummer: Kontonummer,
    val navigering: OpprettKravVeiviserNavigering,
) {
    companion object {
        fun from(
            gjennomforing: GjennomforingGruppetiltak,
            kontonummer: Kontonummer,
        ): OpprettKravUtbetalingsinformasjon {
            return OpprettKravUtbetalingsinformasjon(
                kontonummer = kontonummer,
                navigering = getVeiviserNavigering(
                    OpprettKravVeiviserSteg.UTBETALING,
                    gjennomforing,
                ),
            )
        }
    }
}

@Serializable
data class OpprettKravOppsummeringRequest(
    val periodeStart: String,
    val periodeSlutt: String,
    val periodeInklusiv: Boolean?,
    val kidNummer: String? = null,
    val belop: Int,
)

@Serializable
data class OpprettKravOppsummering(
    val innsendingsInformasjon: List<LabeledDataElement>,
    val utbetalingInformasjon: List<LabeledDataElement>,
    val innsendingsData: InnsendingsData,
    val navigering: OpprettKravVeiviserNavigering,
) {
    companion object {
        fun from(
            requestData: OpprettKravOppsummeringRequest,
            gjennomforing: GjennomforingGruppetiltak,
            kontonummer: Kontonummer?,
        ): OpprettKravOppsummering {
            val periodeStart = LocalDate.parse(requestData.periodeStart)
            val periodeSlutt = LocalDate.parse(requestData.periodeSlutt)
            val periode = if (requestData.periodeInklusiv == true) {
                Periode.fromInclusiveDates(periodeStart, periodeSlutt)
            } else {
                Periode(periodeStart, periodeSlutt)
            }

            return OpprettKravOppsummering(
                innsendingsInformasjon = getInnsendingsInformasjon(gjennomforing),
                utbetalingInformasjon = listOf(
                    LabeledDataElement.text(
                        "Utbetalingsperiode",
                        periode.formatPeriode(),
                    ),
                    LabeledDataElement.text(
                        "Kontonummer",
                        kontonummer?.value ?: "Klarte ikke hente kontonummer",
                    ),
                    LabeledDataElement.text(
                        "KID-nummer",
                        requestData.kidNummer ?: "",
                    ),
                    LabeledDataElement.nok(
                        "Beløp",
                        requestData.belop,
                    ),
                ),
                innsendingsData = InnsendingsData(
                    periode = periode,
                    belop = requestData.belop,
                    kidNummer = requestData.kidNummer,
                    minAntallVedlegg = minAntallVedleggVedOpprettKrav(gjennomforing.prismodell?.type),
                ),
                navigering = getVeiviserNavigering(OpprettKravVeiviserSteg.OPPSUMMERING, gjennomforing),
            )
        }
    }

    @Serializable
    data class InnsendingsData(
        val periode: Periode,
        val belop: Int,
        val kidNummer: String?,
        val minAntallVedlegg: Int,
    )
}

@Serializable
data class OpprettKravUtbetalingRequest(
    @Serializable(with = UUIDSerializer::class)
    val tilsagnId: UUID,
    val periodeStart: String,
    val periodeSlutt: String,
    val kidNummer: String? = null,
    val belop: Int,
    val vedlegg: List<Vedlegg>,
)

private suspend fun RoutingContext.receiveOpprettKravUtbetalingRequest(): Either<List<FieldError>, OpprettKravUtbetalingRequest> = either {
    var tilsagnId: UUID? = null
    var periodeStart: String? = null
    var periodeSlutt: String? = null
    var kidNummer: String? = null
    var belop: Int? = null
    val vedlegg: MutableList<Vedlegg> = mutableListOf()
    val multipart = call.receiveMultipart()

    multipart.forEachPart { part ->
        when (part) {
            is PartData.FormItem -> {
                when (part.name) {
                    "tilsagnId" -> tilsagnId = UUID.fromString(part.value)
                    "kidNummer" -> kidNummer = part.value
                    "belop" -> belop = part.value.toInt()
                    "periodeStart" -> periodeStart = part.value
                    "periodeSlutt" -> periodeSlutt = part.value
                }
            }

            is PartData.FileItem -> {
                if (part.name == "vedlegg") {
                    vedlegg.add(receiveVedleggPart(part).bind())
                }
            }

            else -> {}
        }

        part.dispose()
    }

    val validatedVedlegg = vedlegg.validateVedlegg()

    OpprettKravUtbetalingRequest(
        tilsagnId = requireNotNull(tilsagnId) { "Mangler tilsagnId" },
        periodeStart = requireNotNull(periodeStart) { "Mangler periodeStart" },
        periodeSlutt = requireNotNull(periodeSlutt) { "Mangler periodeSlutt" },
        kidNummer = kidNummer,
        belop = belop ?: 0,
        vedlegg = validatedVedlegg,
    )
}

@Serializable
data class OpprettKravUtbetalingResponse(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
)
