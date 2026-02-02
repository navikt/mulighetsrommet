package no.nav.mulighetsrommet.api.arrangorflate.api

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.getOrElse
import arrow.core.nel
import arrow.core.raise.either
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.route
import io.ktor.server.util.getOrFail
import io.ktor.utils.io.toByteArray
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import no.nav.mulighetsrommet.altinn.AltinnRettigheterService
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.OkonomiConfig
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorInnsendingRadDto
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorflateTilsagnDto
import no.nav.mulighetsrommet.api.arrangorflate.dto.toRadDto
import no.nav.mulighetsrommet.api.arrangorflate.model.ArrangorflateTiltak
import no.nav.mulighetsrommet.api.arrangorflate.service.ArrangorflatePersonalia
import no.nav.mulighetsrommet.api.arrangorflate.service.ArrangorflateService
import no.nav.mulighetsrommet.api.arrangorflate.service.ArrangorflateUtbetalingValidator
import no.nav.mulighetsrommet.api.arrangorflate.service.beregningSatsPeriodeDetaljerUtenFaktor
import no.nav.mulighetsrommet.api.arrangorflate.service.deltakelseCommonCells
import no.nav.mulighetsrommet.api.arrangorflate.service.deltakelseCommonColumns
import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.api.utbetaling.UtbetalingInputHelper.resolveAvtaltPrisPerTimeOppfolgingPerDeltaker
import no.nav.mulighetsrommet.api.utbetaling.UtbetalingService
import no.nav.mulighetsrommet.api.utbetaling.UtbetalingValidator
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakelsePeriode
import no.nav.mulighetsrommet.api.utbetaling.model.Deltaker
import no.nav.mulighetsrommet.api.utbetaling.model.SatsPeriode
import no.nav.mulighetsrommet.api.utbetaling.model.StengtPeriode
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.clamav.ClamAvClient
import no.nav.mulighetsrommet.clamav.Content
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
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import org.koin.ktor.ext.inject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

fun Route.arrangorflateRoutesOpprettKrav(okonomiConfig: OkonomiConfig) {
    val db: ApiDatabase by inject()
    val utbetalingService: UtbetalingService by inject()
    val arrangorFlateService: ArrangorflateService by inject()
    val clamAvClient: ClamAvClient by inject()
    val altinnRettigheterService: AltinnRettigheterService by inject()

    fun requireGjennomforingTilArrangor(
        tiltak: ArrangorflateTiltak,
        organisasjonsnummer: Organisasjonsnummer,
    ): ArrangorflateTiltak = if (tiltak.arrangor.organisasjonsnummer != organisasjonsnummer) {
        throw StatusException(HttpStatusCode.Forbidden, "Ikke gjennomføring til bedrift")
    } else {
        tiltak
    }

    suspend fun RoutingContext.requireArrangorflateTiltak(): ArrangorflateTiltak {
        val orgnr = call.parameters.getOrFail("orgnr").let { Organisasjonsnummer(it) }
        requireTilgangHosArrangor(altinnRettigheterService, orgnr)

        val id = call.parameters.getOrFail("gjennomforingId").let { UUID.fromString(it) }
        val tiltak = db.session { queries.arrangorTiltak.getOrError(id) }
        return requireGjennomforingTilArrangor(tiltak, orgnr)
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
        val tiltak = db.session {
            val gyldigeTiltakstyper = queries.tiltakstype
                .getAll(statuser = listOf(TiltakstypeStatus.AKTIV))
                .map { it.id }

            val gyldigePrismodeller = okonomiConfig.opprettKravPrismodeller

            if (gyldigePrismodeller.isEmpty() || gyldigeTiltakstyper.isEmpty()) {
                emptyList()
            } else {
                queries.arrangorTiltak.getAll(
                    tiltakstyper = gyldigeTiltakstyper,
                    organisasjonsnummer = arrangorer,
                    prismodeller = gyldigePrismodeller,
                    statuser = type.toGjennomforingStatuses(),
                )
            }
        }

        call.respond(
            tiltak.map { it.toRadDto() },
        )
    }

    route("/arrangor/{orgnr}/gjennomforing/{gjennomforingId}/opprett-krav") {
        get({
            description = "Hent opprettkrav data"
            tags = setOf("Arrangorflate")
            operationId = "getOpprettKravData"
            request {
                pathParameter<Organisasjonsnummer>("orgnr")
                pathParameter<String>("gjennomforingId")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Opprett krav data"
                    body<OpprettKravData>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val tiltak = requireArrangorflateTiltak()

            val stegListe = getVeiviserSteg(tiltak)

            val tilsagnstyper = if (tiltak.prismodell.type == PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK) {
                listOf(TilsagnType.INVESTERING)
            } else {
                listOf(TilsagnType.TILSAGN, TilsagnType.EKSTRATILSAGN)
            }
            val tilsagn = arrangorFlateService.getTilsagn(
                arrangorer = setOf(tiltak.arrangor.organisasjonsnummer),
                typer = tilsagnstyper,
                statuser = listOf(TilsagnStatus.GODKJENT),
                gjennomforingId = tiltak.id,
            )

            // TODO: Inkluder filtrering på eksisternde utbetalinger
            // val tidligereUtbetalingsPerioder = db.session { queries.utbetaling.getByGjennomforing(gjennomforing.id) }.map { it.periode }.toSet()
            val innsendingsinformasjon = OpprettKravInnsendingSteg.from(
                okonomiConfig,
                tiltak,
                tilsagn,
                tidligereUtbetalinger = emptyList(),
            )

            val kontonummer = arrangorFlateService.getKontonummer(tiltak.arrangor.organisasjonsnummer)
                .onLeft { return@get call.respondWithProblemDetail(InternalServerError("Klarte ikke å hente kontonummeret")) }
                .getOrElse { throw IllegalStateException("unreachable") }

            call.respond(
                OpprettKravData(
                    steg = stegListe.map { it.toDto() },
                    innsendingSteg = innsendingsinformasjon,
                    utbetalingSteg = OpprettKravUtbetalingSteg.from(
                        tiltak,
                        kontonummer,
                    ),
                    vedleggSteg = OpprettKravVedleggSteg.from(tiltak),
                ),
            )
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
            val tiltak = requireArrangorflateTiltak()

            val periode = getPeriodeFromQuery()

            val avtaltPrisPerTimeOppfolgingPerDeltaker = db.session {
                val gjennomforing = queries.gjennomforing.getGruppetiltakOrError(tiltak.id)
                resolveAvtaltPrisPerTimeOppfolgingPerDeltaker(gjennomforing, periode)
            }

            val personalia = arrangorFlateService.getPersonalia(
                avtaltPrisPerTimeOppfolgingPerDeltaker
                    .deltakelsePerioder.map { it.deltakelseId }.toSet(),
            )

            call.respond(
                OpprettKravDeltakere.from(
                    tiltak,
                    satser = avtaltPrisPerTimeOppfolgingPerDeltaker.satser,
                    stengtHosArrangor = avtaltPrisPerTimeOppfolgingPerDeltaker.stengtHosArrangor,
                    deltakere = avtaltPrisPerTimeOppfolgingPerDeltaker.deltakere,
                    deltakelsePerioder = avtaltPrisPerTimeOppfolgingPerDeltaker.deltakelsePerioder
                        .sortedBy { it.periode.start },
                    personalia,
                ),
            )
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
            val tiltak = requireArrangorflateTiltak()
            receiveOpprettKravUtbetalingRequest()
                .onLeft { return@post call.respondWithProblemDetail(ValidationError(errors = it)) }
                .flatMap { request ->
                    // Scan vedlegg for virus
                    if (clamAvClient.virusScanVedlegg(request.vedlegg).any { it.Result == Status.FOUND }) {
                        return@post call.respondWithProblemDetail(BadRequest("Virus funnet i minst ett vedlegg"))
                    }

                    arrangorFlateService.getKontonummer(tiltak.arrangor.organisasjonsnummer)
                        .mapLeft { FieldError("/kontonummer", "Klarte ikke hente kontonummer").nel() }
                        .flatMap { kontonummer ->
                            ArrangorflateUtbetalingValidator.validateOpprettKravArrangorflate(
                                request,
                                tiltak,
                                okonomiConfig,
                                kontonummer,
                            )
                        }
                        .flatMap { utbetalingService.opprettUtbetaling(it, Arrangor) }
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
data class OpprettKravData(
    val steg: List<OpprettKravVeiviserStegDto>,
    val innsendingSteg: OpprettKravInnsendingSteg,
    val utbetalingSteg: OpprettKravUtbetalingSteg,
    val vedleggSteg: OpprettKravVedleggSteg,
)

@Serializable
enum class OpprettKravVeiviserSteg(val navn: String, val order: Int) {
    INFORMASJON("Innsendingsinformasjon", 1),
    DELTAKERLISTE("Deltakere", 2),
    UTBETALING("Utbetalingsinformasjon", 3),
    VEDLEGG("Vedlegg", 4),
    OPPSUMMERING("Oppsummering", 5),
}

fun getVeiviserSteg(tiltak: ArrangorflateTiltak): List<OpprettKravVeiviserSteg> {
    val stegListe = mutableListOf(
        OpprettKravVeiviserSteg.INFORMASJON,
        OpprettKravVeiviserSteg.UTBETALING,
        OpprettKravVeiviserSteg.VEDLEGG,
        OpprettKravVeiviserSteg.OPPSUMMERING,
    )

    if (tiltak.prismodell.type == PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER) {
        stegListe.add(OpprettKravVeiviserSteg.DELTAKERLISTE)
    }
    stegListe.sortBy { it.order }
    return stegListe
}

fun OpprettKravVeiviserSteg.toDto(): OpprettKravVeiviserStegDto = OpprettKravVeiviserStegDto(type = this, navn = navn, order = order)

@Serializable
data class OpprettKravVeiviserStegDto(val type: OpprettKravVeiviserSteg, val navn: String, val order: Int)

@Serializable
data class OpprettKravInnsendingSteg(
    val guidePanel: GuidePanelType?,
    val definisjonsListe: List<LabeledDataElement>,
    val tilsagn: List<ArrangorflateTilsagnDto>,
    val datoVelger: DatoVelger,
) {
    companion object {
        fun from(
            okonomiConfig: OkonomiConfig,
            tiltak: ArrangorflateTiltak,
            tilsagn: List<ArrangorflateTilsagnDto>,
            tidligereUtbetalinger: List<Utbetaling>,
        ): OpprettKravInnsendingSteg {
            val datoVelger = DatoVelger.from(
                okonomiConfig,
                tiltak,
                tidligereUtbetalingsPerioder = tidligereUtbetalinger.map { it.periode }.toSet(),
            )

            return OpprettKravInnsendingSteg(
                guidePanel = panelGuide(tiltak.prismodell.type),
                definisjonsListe = getInnsendingsInformasjon(tiltak),
                tilsagn = tilsagn,
                datoVelger = datoVelger,
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

fun getInnsendingsInformasjon(tiltak: ArrangorflateTiltak): List<LabeledDataElement> {
    val standardList = listOf(
        LabeledDataElement.text(
            "Arrangør",
            "${tiltak.arrangor.navn} - ${tiltak.arrangor.organisasjonsnummer.value}",
        ),
        LabeledDataElement.text("Tiltaksnavn", "${tiltak.navn} (${tiltak.lopenummer.value})"),
        LabeledDataElement.text("Tiltakstype", tiltak.tiltakstype.navn),
    )
    if (tiltak.prismodell.type == PrismodellType.ANNEN_AVTALT_PRIS) {
        return standardList +
            LabeledDataElement.text(
                "Tiltaksperiode",
                Periode.formatPeriode(tiltak.startDato, tiltak.sluttDato!!),
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
            tiltak: ArrangorflateTiltak,
            tidligereUtbetalingsPerioder: Set<Periode> = emptySet(),
        ): DatoVelger {
            when (tiltak.prismodell.type) {
                PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER -> {
                    // Har de nådd innsendingssteget, kan vi garantere at tiltakskoden er konfigurert opp
                    val tilsagnPeriode =
                        okonomiConfig.gyldigTilsagnPeriode[tiltak.tiltakstype.tiltakskode]!!

                    val firstOfThisMonth = LocalDate.now().withDayOfMonth(1)
                    val perioder = Periode(
                        start = maxOf(tilsagnPeriode.start, tiltak.startDato),
                        slutt = minOf(firstOfThisMonth, tiltak.sluttDato ?: firstOfThisMonth),
                    ).splitByMonth()
                    val filtrertePerioder =
                        perioder.filter { it !in tidligereUtbetalingsPerioder }.sortedBy { it.start }
                    return DatoSelect(filtrertePerioder)
                }

                PrismodellType.ANNEN_AVTALT_PRIS,
                PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK,
                -> {
                    return DatoRange(
                        ArrangorflateUtbetalingValidator.maksUtbetalingsPeriodeSluttDato(
                            tiltak,
                            okonomiConfig,
                        ),
                    )
                }

                else -> throw StatusException(
                    HttpStatusCode.Forbidden,
                    "Du kan ikke opprette utbetalingskrav for denne tiltaksgjennomføringen",
                )
            }
        }
    }
}

@Serializable
data class OpprettKravVedleggSteg(
    val guidePanel: GuidePanelType?,
    val minAntallVedlegg: Int,
) {
    companion object {
        fun from(tiltak: ArrangorflateTiltak): OpprettKravVedleggSteg {
            return OpprettKravVedleggSteg(
                guidePanel = GuidePanelType.from(tiltak.prismodell.type),
                minAntallVedlegg = UtbetalingValidator.MIN_ANTALL_VEDLEGG_OPPRETT_KRAV,
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
) {
    companion object {
        fun from(
            tiltak: ArrangorflateTiltak,
            satser: Set<SatsPeriode>,
            stengtHosArrangor: Set<StengtPeriode>,
            deltakere: List<Deltaker>,
            deltakelsePerioder: List<DeltakelsePeriode>,
            personalia: Map<UUID, ArrangorflatePersonalia>,
        ): OpprettKravDeltakere {
            return OpprettKravDeltakere(
                guidePanel = GuidePanelType.from(tiltak.prismodell.type),
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
data class OpprettKravUtbetalingSteg(
    val kontonummer: Kontonummer,
    val valuta: Valuta,
) {
    companion object {
        fun from(
            tiltak: ArrangorflateTiltak,
            kontonummer: Kontonummer,
        ): OpprettKravUtbetalingSteg {
            return OpprettKravUtbetalingSteg(
                kontonummer = kontonummer,
                valuta = tiltak.prismodell.valuta,
            )
        }
    }
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
        belop = belop ?: 0, // Valuta hentes fra prismodell
        vedlegg = validatedVedlegg,
    )
}

@Serializable
data class OpprettKravUtbetalingResponse(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
)

fun MutableList<Vedlegg>.validateVedlegg(): List<Vedlegg> {
    return this.map { v ->
        // Optionally validate file type and size here
        val fileName = v.filename
        val contentType = v.content.contentType

        require(contentType.equals("application/pdf", ignoreCase = true)) {
            "Vedlegg $fileName er ikke en PDF"
        }

        v
    }
}

const val VEDLEGG_MAX_SIZE_BYTES = 10 * 1024 * 1024

suspend fun receiveVedleggPart(part: PartData.FileItem): Either<List<FieldError>, Vedlegg> = either {
    val vedlegg = Vedlegg(
        content = Content(
            contentType = part.contentType.toString(),
            content = part.provider().toByteArray(),
        ),
        filename = part.originalFileName ?: "ukjent.pdf",
    )
    if (vedlegg.content.content.size > VEDLEGG_MAX_SIZE_BYTES) {
        raise(listOf(FieldError("/vedlegg", "Vedlegg er større enn 10MB")))
    }
    vedlegg
}
