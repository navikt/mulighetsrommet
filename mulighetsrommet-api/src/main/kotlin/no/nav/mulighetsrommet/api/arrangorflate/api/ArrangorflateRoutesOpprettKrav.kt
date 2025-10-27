package no.nav.mulighetsrommet.api.arrangorflate.api

import arrow.core.flatMap
import arrow.core.nel
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.http.content.default
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.route
import io.ktor.server.util.getOrFail
import io.ktor.utils.io.toByteArray
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.OkonomiConfig
import no.nav.mulighetsrommet.api.arrangorflate.ArrangorflateService
import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
import no.nav.mulighetsrommet.api.clients.amtDeltaker.DeltakerPersonalia
import no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.api.tiltakstype.model.TiltakstypeDto
import no.nav.mulighetsrommet.api.utbetaling.UtbetalingInputHelper.resolveAvtaltPrisPerTimeOppfolgingPerDeltaker
import no.nav.mulighetsrommet.api.utbetaling.UtbetalingService
import no.nav.mulighetsrommet.api.utbetaling.UtbetalingValidator
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakelsePeriode
import no.nav.mulighetsrommet.api.utbetaling.model.Deltaker
import no.nav.mulighetsrommet.api.utbetaling.model.StengtPeriode
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.clamav.ClamAvClient
import no.nav.mulighetsrommet.clamav.Content
import no.nav.mulighetsrommet.clamav.Status
import no.nav.mulighetsrommet.clamav.Vedlegg
import no.nav.mulighetsrommet.ktor.exception.BadRequest
import no.nav.mulighetsrommet.ktor.exception.StatusException
import no.nav.mulighetsrommet.ktor.plugins.respondWithProblemDetail
import no.nav.mulighetsrommet.model.Arrangor
import no.nav.mulighetsrommet.model.DataDrivenTableDto
import no.nav.mulighetsrommet.model.DataElement
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.ProblemDetail
import no.nav.mulighetsrommet.model.TiltakstypeStatus
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import org.apache.xmlbeans.impl.soap.Detail
import org.koin.ktor.ext.inject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.getValue

fun Route.arrangorflateRoutesOpprettKrav(okonomiConfig: OkonomiConfig) {
    val db: ApiDatabase by inject()
    val utbetalingService: UtbetalingService by inject()
    val arrangorFlateService: ArrangorflateService by inject()
    val clamAvClient: ClamAvClient by inject()

    fun requireGjennomforingTilArrangor(
        gjennomforing: Gjennomforing,
        organisasjonsnummer: Organisasjonsnummer,
    ) = if (gjennomforing.arrangor.organisasjonsnummer != organisasjonsnummer) {
        throw StatusException(HttpStatusCode.Forbidden, "Ikke gjennomføring til bedrift")
    } else {
        Unit
    }

    fun RoutingContext.getPeriodeFromQuery(): Periode {
        val periodeStart = call.queryParameters["periodeStart"]?.let { start ->
            LocalDate.parse(
                start,
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            )
        }
        val periodeSlutt = call.queryParameters["periodeSlutt"]?.let { start ->
            LocalDate.parse(
                start,
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            )
        }
        return periodeStart?.let { start -> periodeSlutt?.let { slutt -> Periode.of(start, slutt) } }
            ?: throw StatusException(HttpStatusCode.BadRequest, "Periode er ikke oppgitt")
    }

    get("/opprett-krav", {
        description = "Hent gjennomføringene til arrangør - tabell format"
        tags = setOf("Arrangorflate")
        operationId = "getArrangorflateGjennomforingerTabeller"
        request {
            pathParameter<Organisasjonsnummer>("orgnr")
        }
        response {
            code(HttpStatusCode.OK) {
                description = "Arrangør sine gjennomføringer (DataDrivenTable)"
                body<GjennomforingerTableResponse>()
            }
            default {
                description = "Problem details"
                body<ProblemDetail>()
            }
        }
    }) {
        val orgnr = call.parameters.getOrFail("orgnr").let { Organisasjonsnummer(it) }
        requireTilgangHosArrangor(orgnr)

        val gjennomforinger = db.session {
            val aktiveTiltakstyper = queries.tiltakstype.getAll(statuser = listOf(TiltakstypeStatus.AKTIV))
            val opprettKravPrismodeller = hentOpprettKravPrismodeller(okonomiConfig)
            val opprettKravTiltakstyperMedTilsagn = hentTiltakstyperMedTilsagn(okonomiConfig, aktiveTiltakstyper)

            if (opprettKravPrismodeller.isEmpty() || opprettKravTiltakstyperMedTilsagn.isEmpty()) {
                return@session emptyList()
            } else {
                queries.gjennomforing
                    .getAll(
                        arrangorOrgnr = listOf(orgnr),
                        prismodeller = opprettKravPrismodeller,
                        tiltakstypeIder = opprettKravTiltakstyperMedTilsagn,
                    )
                    .items
            }
        }
        call.respond(
            toGjennomforingerTableResponse(orgnr, gjennomforinger),
        )
    }

    route("/{gjennomforingId}/opprett-krav") {
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
            val orgnr = call.parameters.getOrFail("orgnr").let { Organisasjonsnummer(it) }
            requireTilgangHosArrangor(orgnr)
            val gjennomforingId = call.parameters.getOrFail("gjennomforingId").let { UUID.fromString(it) }

            val gjennomforing = requireNotNull(
                db.session {
                    queries.gjennomforing
                        .get(
                            id = gjennomforingId,
                        )
                },
            )
            requireGjennomforingTilArrangor(gjennomforing, orgnr)

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
            val orgnr = call.parameters.getOrFail("orgnr").let { Organisasjonsnummer(it) }
            requireTilgangHosArrangor(orgnr)
            val gjennomforingId = call.parameters.getOrFail("gjennomforingId").let { UUID.fromString(it) }

            val gjennomforing = requireNotNull(
                db.session {
                    queries.gjennomforing
                        .get(
                            id = gjennomforingId,
                        )
                },
            )
            requireGjennomforingTilArrangor(gjennomforing, orgnr)

            val tilsagnsTyper =
                if (gjennomforing.avtalePrismodell == PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK) {
                    listOf(TilsagnType.INVESTERING)
                } else {
                    listOf(TilsagnType.TILSAGN, TilsagnType.EKSTRATILSAGN)
                }
            val tilsagn = arrangorFlateService.getTilsagn(
                ArrangorflateTilsagnFilter(
                    typer = tilsagnsTyper,
                    statuser = listOf(TilsagnStatus.GODKJENT),
                    gjennomforingId = gjennomforingId,
                ),
                orgnr,
            )

            // TODO: Ikluder filtrering på eksisternde utbetalinger
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
            val orgnr = call.parameters.getOrFail("orgnr").let { Organisasjonsnummer(it) }
            requireTilgangHosArrangor(orgnr)
            val gjennomforingId = call.parameters.getOrFail("gjennomforingId").let { UUID.fromString(it) }

            val gjennomforing = requireNotNull(
                db.session {
                    queries.gjennomforing
                        .get(
                            id = gjennomforingId,
                        )
                },
            )
            requireGjennomforingTilArrangor(gjennomforing, orgnr)

            val periode = getPeriodeFromQuery()

            val avtaltPrisPerTimeOppfolgingPerDeltaker =
                db.session { resolveAvtaltPrisPerTimeOppfolgingPerDeltaker(gjennomforing, periode) }
            val deltakere = avtaltPrisPerTimeOppfolgingPerDeltaker.deltakere
            val deltakelsePerioder =
                avtaltPrisPerTimeOppfolgingPerDeltaker.deltakelsePerioder.sortedBy { it.periode.start }
            val sats = avtaltPrisPerTimeOppfolgingPerDeltaker.sats
            val stengtHosArrangor = avtaltPrisPerTimeOppfolgingPerDeltaker.stengtHosArrangor

            val personalia = arrangorFlateService.getPersonalia(deltakelsePerioder.map { it.deltakelseId })

            val table = createDeltakerTable(deltakere, deltakelsePerioder, personalia)
            val tableFooter = listOf(
                DetailsEntry.number("Antall deltakere", deltakelsePerioder.size),
                DetailsEntry.nok("Pris", sats),
            )
            val navigering = getVeiviserNavigering(OpprettKravVeiviserSteg.DELTAKERLISTE, gjennomforing)

            call.respond(OpprettKravDeltakere(stengtHosArrangor, table, tableFooter, navigering))
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
            val orgnr = call.parameters.getOrFail("orgnr").let { Organisasjonsnummer(it) }
            requireTilgangHosArrangor(orgnr)
            val gjennomforingId = call.parameters.getOrFail("gjennomforingId").let { UUID.fromString(it) }
            val gjennomforing = requireNotNull(
                db.session {
                    queries.gjennomforing
                        .get(
                            id = gjennomforingId,
                        )
                },
            )
            requireGjennomforingTilArrangor(gjennomforing, orgnr)

            val request = call.receive<OpprettKravOppsummeringRequest>()

            val periodeStart = LocalDate.parse(request.periodeStart)
            val periodeSlutt = LocalDate.parse(request.periodeSlutt)
            val periode = if (request.periodeInklusiv == true) {
                Periode.fromInclusiveDates(periodeStart, periodeSlutt)
            } else {
                Periode(periodeStart, periodeSlutt)
            }
            val kontonummer = arrangorFlateService.getKontonummer(orgnr).getOrNull()
            call.respond(
                OpprettKravOppsummering(
                    innsendingsInformasjon = listOf(
                        DetailsEntry(
                            key = "Arrangør",
                            value = "${gjennomforing.arrangor.navn} - ${orgnr.value}",
                        ),
                        DetailsEntry(
                            key = "Tiltaksnavn",
                            value = gjennomforing.navn,
                        ),
                        DetailsEntry(
                            key = "Tiltakstype",
                            value = gjennomforing.tiltakstype.navn,
                        ),
                    ),
                    utbetalingInformasjon = listOf(
                        DetailsEntry(
                            key = "UtbetalingsPeriode",
                            value = periode.formatPeriode(),
                        ),
                        DetailsEntry(
                            key = "Kontonummer",
                            value = kontonummer?.value ?: "Klarte ikke hente kontonummer",
                        ),
                        DetailsEntry(
                            key = "KID-nummer",
                            value = request.kidNummer ?: "",
                        ),
                    ),
                    innsendingsData = OpprettKravOppsummering.InnsendingsData(
                        periode = periode,
                        belop = request.belop,
                        kidNummer = request.kidNummer,
                    ),
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
            val orgnr = call.parameters.getOrFail("orgnr").let { Organisasjonsnummer(it) }

            requireTilgangHosArrangor(orgnr)
            val request = receiveOpprettKravUtbetalingRequest(call)

            val gjennomforingId = call.parameters.getOrFail("gjennomforingId").let { UUID.fromString(it) }
            val gjennomforing = requireNotNull(
                db.session {
                    queries.gjennomforing
                        .get(
                            id = gjennomforingId,
                        )
                },
            )
            requireGjennomforingTilArrangor(gjennomforing, orgnr)

            // Scan vedlegg for virus
            if (clamAvClient.virusScanVedlegg(request.vedlegg).any { it.Result == Status.FOUND }) {
                return@post call.respondWithProblemDetail(BadRequest("Virus funnet i minst ett vedlegg"))
            }

            if (gjennomforing.avtalePrismodell !in hentOpprettKravPrismodeller(okonomiConfig)) {
                throw StatusException(
                    HttpStatusCode.Forbidden,
                    "Du kan ikke opprette utbetalingskrav for denne tiltaksgjennomføringen",
                )
            }

            arrangorFlateService.getKontonummer(orgnr)
                .mapLeft { FieldError("/kontonummer", "Klarte ikke hente kontonummer").nel() }
                .flatMap { UtbetalingValidator.validateOpprettKravArrangorflate(request, it) }
                .flatMap { utbetalingService.opprettUtbetaling(it, gjennomforing, Arrangor) }
                .onLeft { errors ->
                    call.respondWithProblemDetail(ValidationError("Klarte ikke opprette utbetaling", errors))
                }
                .onRight { utbetaling -> call.respond(OpprettKravOmUtbetalingResponse(utbetaling.id)) }
        }
    }
}

private fun hentOpprettKravPrismodeller(okonomiConfig: OkonomiConfig): List<PrismodellType> {
    val now = LocalDate.now()
    return okonomiConfig.opprettKravPeriode.entries.mapNotNull { entry ->
        if (entry.value.start.isBefore(now) && entry.value.slutt.isAfter(now)) {
            entry.key
        } else {
            null
        }
    }
}

private fun hentTiltakstyperMedTilsagn(okonomiConfig: OkonomiConfig, tiltakstyper: List<TiltakstypeDto>): List<UUID> {
    val now = LocalDate.now()
    return okonomiConfig.gyldigTilsagnPeriode.entries.mapNotNull { tiltakstypeMedTilsagnPeriode ->
        if (tiltakstypeMedTilsagnPeriode.value.contains(now)) {
            tiltakstyper.find { it.tiltakskode == tiltakstypeMedTilsagnPeriode.key }?.id
        } else {
            null
        }
    }
}

@Serializable
data class GjennomforingerTableResponse(
    val aktive: DataDrivenTableDto,
    val historiske: DataDrivenTableDto,
)

private fun toGjennomforingerTableResponse(
    orgNr: Organisasjonsnummer,
    gjennomforinger: List<Gjennomforing>,
): GjennomforingerTableResponse {
    val aktive = gjennomforinger.filter { it.status.type == GjennomforingStatusType.GJENNOMFORES }
    val historiske = gjennomforinger.filter { it.status.type != GjennomforingStatusType.GJENNOMFORES }

    return GjennomforingerTableResponse(
        aktive = toGjennomforingDataTable(orgNr, aktive),
        historiske = toGjennomforingDataTable(orgNr, historiske),
    )
}

private fun toGjennomforingDataTable(
    orgNr: Organisasjonsnummer,
    gjennomforinger: List<Gjennomforing>,
): DataDrivenTableDto {
    return DataDrivenTableDto(
        columns = listOf(
            DataDrivenTableDto.Column("navn", "Tiltaksnavn"),
            DataDrivenTableDto.Column("tiltaksnummer", "Tiltaksnr."),
            DataDrivenTableDto.Column("tiltaksType", "Tiltakstype"),
            DataDrivenTableDto.Column("startDato", "Startdato"),
            DataDrivenTableDto.Column("sluttDato", "Sluttdato"),
            DataDrivenTableDto.Column("action", null, sortable = false, align = DataDrivenTableDto.Column.Align.CENTER),
        ),
        rows = gjennomforinger.map { gjennomforing ->
            mapOf(
                "navn" to DataElement.text(gjennomforing.navn),
                "tiltaksnummer" to DataElement.text(gjennomforing.tiltaksnummer),
                "tiltaksType" to DataElement.text(gjennomforing.tiltakstype.navn),
                "startDato" to DataElement.date(gjennomforing.startDato),
                "sluttDato" to DataElement.date(gjennomforing.sluttDato),
                "action" to
                    DataElement.Link(
                        text = "Start innsending",
                        href = hrefOpprettKravInnsendingsInformasjon(orgNr, gjennomforing.id),
                    ),
            )
        },
    )
}

private fun hrefOpprettKravInnsendingsInformasjon(orgnr: Organisasjonsnummer, gjennomforingId: UUID) = "/${orgnr.value}/opprett-krav/$gjennomforingId/innsendingsinformasjon"

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

fun getVeiviserSteg(gjennomforing: Gjennomforing): List<OpprettKravVeiviserSteg> {
    val stegListe = mutableListOf(
        OpprettKravVeiviserSteg.INFORMASJON,
        OpprettKravVeiviserSteg.UTBETALING,
        OpprettKravVeiviserSteg.VEDLEGG,
        OpprettKravVeiviserSteg.OPPSUMMERING,
    )

    if (gjennomforing.avtalePrismodell == PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER) {
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

fun getVeiviserNavigering(steg: OpprettKravVeiviserSteg, gjennomforing: Gjennomforing): OpprettKravVeiviserNavigering {
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
    val definisjonsListe: List<DetailsEntry>,
    val tilsagn: List<ArrangorflateTilsagnDto>,
    val datoVelger: DatoVelger,
    val navigering: OpprettKravVeiviserNavigering,
) {
    companion object {
        fun from(
            okonomiConfig: OkonomiConfig,
            gjennomforing: Gjennomforing,
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
                guidePanel = panelGuide(gjennomforing.avtalePrismodell),
                definisjonsListe = definisjonsListe(gjennomforing),
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

        fun definisjonsListe(gjennomforing: Gjennomforing): List<DetailsEntry> = listOf(
            DetailsEntry(
                "Arrangør",
                "${gjennomforing.arrangor.navn} - ${gjennomforing.arrangor.organisasjonsnummer.value}",
            ),
            DetailsEntry("Tiltaksnavn", gjennomforing.navn),
            DetailsEntry("Tiltakstype", gjennomforing.tiltakstype.navn),
        )
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
        data class DatoRange(val todo: String = "hello") : DatoVelger()

        companion object {
            fun from(
                okonomiConfig: OkonomiConfig,
                gjennomforing: Gjennomforing,
                tidligereUtbetalingsPerioder: Set<Periode> = emptySet(),
            ): DatoVelger {
                if (gjennomforing.avtalePrismodell == PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER && gjennomforing.tiltakstype.tiltakskode in okonomiConfig.gyldigTilsagnPeriode) {
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
                } else {
                    return DatoRange()
                }
            }
        }
    }
}

@Serializable
data class OpprettKravDeltakere(
    val stengtHosArrangor: Set<StengtPeriode>,
    val tabell: DataDrivenTableDto,
    val tabellFooter: List<DetailsEntry>,
    val navigering: OpprettKravVeiviserNavigering,
)

fun createDeltakerTable(
    deltakere: List<Deltaker>,
    deltakelsePerioder: List<DeltakelsePeriode>,
    personalia: Map<UUID, DeltakerPersonalia>,
): DataDrivenTableDto {
    val periodeMap = deltakelsePerioder.associateBy { it.deltakelseId }
    val deltakerMap = deltakere.associateBy { it.id }
    return DataDrivenTableDto(
        columns = listOf(
            DataDrivenTableDto.Column("navn", "Navn", sortable = false),
            DataDrivenTableDto.Column("identitetsnummer", "Fødselsnr.", sortable = false),
            DataDrivenTableDto.Column("tiltakStart", "Startdato i tiltaket", sortable = false),
            DataDrivenTableDto.Column("periodeStart", "Startdato i perioden"),
            DataDrivenTableDto.Column("periodeSlutt", "Sluttdato i perioden"),
        ),
        rows = periodeMap.map { (key, value) ->
            val deltaker = deltakerMap[key]
            val personalia = personalia[key]
            mapOf(
                "navn" to DataElement.text(personalia?.navn),
                "identitetsnummer" to DataElement.text(personalia?.norskIdent?.value),
                "tiltakStart" to DataElement.date(deltaker?.startDato),
                "periodeStart" to DataElement.date(value.periode.start),
                "periodeSlutt" to DataElement.date(value.periode.slutt),
            )
        },
    )
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
    val innsendingsInformasjon: List<DetailsEntry>,
    val utbetalingInformasjon: List<DetailsEntry>,
    val innsendingsData: InnsendingsData,
) {
    @Serializable
    data class InnsendingsData(
        val periode: Periode,
        val belop: Int,
        val kidNummer: String?,
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

private suspend fun receiveOpprettKravUtbetalingRequest(call: RoutingCall): OpprettKravUtbetalingRequest {
    var tilsagnId: UUID? = null
    var periodeStart: String? = null
    var periodeSlutt: String? = null
    var kidNummer: String? = null
    var belop: Int? = null
    val vedlegg: MutableList<Vedlegg> = mutableListOf()
    val multipart = call.receiveMultipart(formFieldLimit = 1024 * 1024 * 100)

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
                    vedlegg.add(
                        Vedlegg(
                            content = Content(
                                contentType = part.contentType.toString(),
                                content = part.provider().toByteArray(),
                            ),
                            filename = part.originalFileName ?: "ukjent.pdf",
                        ),
                    )
                }
            }

            else -> {}
        }

        part.dispose()
    }

    val validatedVedlegg = vedlegg.validateVedlegg()

    return OpprettKravUtbetalingRequest(
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
