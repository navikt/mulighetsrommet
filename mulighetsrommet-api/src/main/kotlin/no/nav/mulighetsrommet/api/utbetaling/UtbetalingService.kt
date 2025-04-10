package no.nav.mulighetsrommet.api.utbetaling

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.common.kafka.producer.feilhandtering.StoredProducerRecord
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.arrangorflate.api.GodkjennUtbetaling
import no.nav.mulighetsrommet.api.clients.kontoregisterOrganisasjon.KontoregisterOrganisasjonClient
import no.nav.mulighetsrommet.api.endringshistorikk.DocumentClass
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingDto
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsattRolle
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.responses.StatusResponse
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.tilsagn.TilsagnService
import no.nav.mulighetsrommet.api.tilsagn.model.ForhandsgodkjenteSatser
import no.nav.mulighetsrommet.api.tilsagn.model.Tilsagn
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Besluttelse
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.api.utbetaling.api.BesluttDelutbetalingRequest
import no.nav.mulighetsrommet.api.utbetaling.api.OpprettDelutbetalingerRequest
import no.nav.mulighetsrommet.api.utbetaling.db.DelutbetalingDbo
import no.nav.mulighetsrommet.api.utbetaling.db.UtbetalingDbo
import no.nav.mulighetsrommet.api.utbetaling.model.*
import no.nav.mulighetsrommet.api.utbetaling.task.JournalforUtbetaling
import no.nav.mulighetsrommet.ktor.exception.NotFound
import no.nav.mulighetsrommet.model.*
import no.nav.tiltak.okonomi.OkonomiBestillingMelding
import no.nav.tiltak.okonomi.OpprettFaktura
import no.nav.tiltak.okonomi.toOkonomiPart
import org.intellij.lang.annotations.Language
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

val AUTOMATISK_RETURNERT_AARSAK: String = "AUTOMATISK_RETURNERT"

class UtbetalingService(
    private val config: Config,
    private val db: ApiDatabase,
    private val tilsagnService: TilsagnService,
    private val journalforUtbetaling: JournalforUtbetaling,
    private val kontoregisterOrganisasjonClient: KontoregisterOrganisasjonClient,
) {
    data class Config(
        val bestillingTopic: String,
    )

    private val log: Logger = LoggerFactory.getLogger(javaClass.simpleName)

    suspend fun genererUtbetalingForMonth(date: LocalDate): List<Utbetaling> = db.transaction {
        val periode = Periode.forMonthOf(date)

        getGjennomforingerForGenereringAvUtbetalinger(periode)
            .mapNotNull { (gjennomforingId, avtaletype) ->
                val utbetaling = when (avtaletype) {
                    Avtaletype.Forhaandsgodkjent -> createUtbetalingForhandsgodkjent(
                        utbetalingId = UUID.randomUUID(),
                        gjennomforingId = gjennomforingId,
                        periode = periode,
                    )

                    else -> null
                }
                utbetaling?.takeIf { it.beregning.output.belop > 0 }
            }
            .map { utbetaling ->
                queries.utbetaling.upsert(utbetaling)
                val dto = getOrError(utbetaling.id)
                logEndring("Utbetaling opprettet", dto, Tiltaksadministrasjon)
                dto
            }
    }

    suspend fun oppdaterUtbetalingBeregningForGjennomforing(id: UUID): Unit = db.transaction {
        queries.utbetaling
            .getByGjennomforing(id)
            .filter { it.innsender == null }
            .mapNotNull { gjeldendeKrav ->
                val nyttKrav = when (gjeldendeKrav.beregning) {
                    is UtbetalingBeregningForhandsgodkjent -> createUtbetalingForhandsgodkjent(
                        utbetalingId = gjeldendeKrav.id,
                        gjennomforingId = gjeldendeKrav.gjennomforing.id,
                        periode = gjeldendeKrav.beregning.input.periode,
                    )

                    is UtbetalingBeregningFri -> null
                }

                nyttKrav?.takeIf { it.beregning != gjeldendeKrav.beregning }
            }
            .forEach { utbetaling ->
                queries.utbetaling.upsert(utbetaling)
                val dto = getOrError(utbetaling.id)
                logEndring("Utbetaling beregning oppdatert", dto, Tiltaksadministrasjon)
            }
    }

    suspend fun createUtbetalingForhandsgodkjent(
        utbetalingId: UUID,
        gjennomforingId: UUID,
        periode: Periode,
    ): UtbetalingDbo = db.session {
        val frist = periode.slutt.plusMonths(2)

        val gjennomforing = requireNotNull(queries.gjennomforing.get(gjennomforingId))

        val sats = ForhandsgodkjenteSatser.findSats(gjennomforing.tiltakstype.tiltakskode, periode.start)
            ?: throw IllegalStateException("Sats mangler for periode $periode")

        val stengtHosArrangor = resolveStengtHosArrangor(periode, gjennomforing.stengt)

        val deltakelser = resolveDeltakelser(gjennomforingId, periode)

        val input = UtbetalingBeregningForhandsgodkjent.Input(
            periode = periode,
            sats = sats,
            stengt = stengtHosArrangor,
            deltakelser = deltakelser,
        )

        val beregning = UtbetalingBeregningForhandsgodkjent.beregn(input)

        val forrigeKrav = queries.utbetaling.getSisteGodkjenteUtbetaling(gjennomforingId)

        val kontonummer = when (
            val result = kontoregisterOrganisasjonClient.getKontonummerForOrganisasjon(
                organisasjonsnummer = gjennomforing.arrangor.organisasjonsnummer,
            )
        ) {
            is Either.Left -> {
                log.error(
                    "Kunne ikke hente kontonummer for organisasjon ${gjennomforing.arrangor.organisasjonsnummer}. Error: {}",
                    result.value,
                )
                null
            }

            is Either.Right -> Kontonummer(result.value.kontonr)
        }

        return UtbetalingDbo(
            id = utbetalingId,
            fristForGodkjenning = frist.atStartOfDay(),
            gjennomforingId = gjennomforingId,
            beregning = beregning,
            kontonummer = kontonummer,
            kid = forrigeKrav?.betalingsinformasjon?.kid,
            periode = periode,
            innsender = null,
            beskrivelse = null,
        )
    }

    // TODO: må verifisere at utbetaling ikke kan godkjennes flere ganger
    fun godkjentAvArrangor(
        utbetalingId: UUID,
        request: GodkjennUtbetaling,
    ) = db.transaction {
        queries.utbetaling.setGodkjentAvArrangor(utbetalingId, LocalDateTime.now())
        queries.utbetaling.setBetalingsinformasjon(
            utbetalingId,
            request.betalingsinformasjon.kontonummer,
            request.betalingsinformasjon.kid,
        )
        val dto = getOrError(utbetalingId)
        logEndring("Utbetaling sendt inn", dto, Arrangor)
        journalforUtbetaling.schedule(utbetalingId, Instant.now(), session as TransactionalSession)
        automatiskUtbetaling(utbetalingId)
    }

    fun opprettManuellUtbetaling(
        request: UtbetalingValidator.ValidatedManuellUtbetalingRequest,
        agent: Agent,
    ): UUID = db.transaction {
        queries.utbetaling.upsert(
            UtbetalingDbo(
                id = request.id,
                gjennomforingId = request.gjennomforingId,
                fristForGodkjenning = request.periodeSlutt.plusMonths(2).atStartOfDay(),
                kontonummer = request.kontonummer,
                kid = request.kidNummer,
                beregning = UtbetalingBeregningFri.beregn(
                    input = UtbetalingBeregningFri.Input(
                        belop = request.belop,
                    ),
                ),
                periode = Periode.fromInclusiveDates(
                    request.periodeStart,
                    request.periodeSlutt,
                ),
                innsender = agent,
                beskrivelse = request.beskrivelse,
            ),
        )
        val dto = getOrError(request.id)
        logEndring("Utbetaling sendt inn", dto, agent)
        dto.id
    }

    fun opprettDelutbetalinger(
        request: OpprettDelutbetalingerRequest,
        navIdent: NavIdent,
    ): StatusResponse<Unit> = db.transaction {
        val utbetaling = queries.utbetaling.get(request.utbetalingId)
            ?: return NotFound("Utbetaling med id=$request.utbetalingId finnes ikke").left()

        UtbetalingValidator.validateOpprettDelutbetalinger(
            utbetaling,
            request.delutbetalinger.map { req ->
                val previous = queries.delutbetaling.get(req.id)
                val tilsagn = requireNotNull(queries.tilsagn.get(req.tilsagnId))
                UtbetalingValidator.OpprettDelutbetaling(
                    id = req.id,
                    gjorOppTilsagn = req.gjorOppTilsagn,
                    previous = previous,
                    tilsagn = tilsagn,
                    belop = req.belop,
                )
            },
        )
            .onLeft { return ValidationError(errors = it).left() }
            .onRight {
                it.forEach {
                    upsertDelutbetaling(utbetaling, it.tilsagn, it.id, it.belop, it.gjorOppTilsagn, navIdent)
                }
            }

        // Slett de som ikke er med i requesten
        queries.delutbetaling.getByUtbetalingId(utbetaling.id)
            .filter { it.id !in request.delutbetalinger.map { it.id } }
            .forEach {
                require(it.status == DelutbetalingStatus.RETURNERT) {
                    "Fatal! Delutbetaling kan ikke slettes fordi den har status: ${it.status}"
                }
                queries.delutbetaling.delete(it.id)
            }

        logEndring(
            "Utbetaling sendt til godkjenning",
            getOrError(utbetaling.id),
            navIdent,
        )

        return Unit.right()
    }

    fun besluttDelutbetaling(
        id: UUID,
        request: BesluttDelutbetalingRequest,
        navIdent: NavIdent,
    ): StatusResponse<Unit> = db.transaction {
        val delutbetaling = requireNotNull(queries.delutbetaling.get(id))
        require(delutbetaling.status == DelutbetalingStatus.TIL_GODKJENNING) {
            "Utbetaling er allerede besluttet"
        }

        val opprettelse = queries.totrinnskontroll.getOrError(delutbetaling.id, Totrinnskontroll.Type.OPPRETT)
        if (navIdent == opprettelse.behandletAv) {
            return ValidationError(errors = listOf(FieldError.root("Kan ikke attestere en utbetaling du selv har opprettet"))).left()
        }

        val tilsagnOpprettelse =
            queries.totrinnskontroll.getOrError(delutbetaling.tilsagnId, Totrinnskontroll.Type.OPPRETT)
        if (navIdent == tilsagnOpprettelse.besluttetAv) {
            return ValidationError(errors = listOf(FieldError.root("Kan ikke attestere en utbetaling der du selv har besluttet tilsagnet"))).left()
        }

        val kostnadssted = checkNotNull(queries.tilsagn.get(delutbetaling.tilsagnId)).kostnadssted
        val ansatt = checkNotNull(queries.ansatt.getByNavIdent(navIdent))
        if (!ansatt.hasRole(NavAnsattRolle.AttestantUtbetaling(setOf(kostnadssted.enhetsnummer)))) {
            return ValidationError(errors = listOf(FieldError.root("Kan ikke attestere utbetalingen fordi du ikke er attstant ved tilsagnets kostnadssted (${kostnadssted.navn})"))).left()
        }

        when (request) {
            is BesluttDelutbetalingRequest.AvvistDelutbetalingRequest -> {
                returnerDelutbetaling(delutbetaling, request.aarsaker, request.forklaring, navIdent)
            }

            is BesluttDelutbetalingRequest.GodkjentDelutbetalingRequest -> {
                godkjennDelutbetaling(delutbetaling, navIdent)
            }
        }

        Unit.right()
    }

    private fun QueryContext.getGjennomforingerForGenereringAvUtbetalinger(
        periode: Periode,
    ): List<Pair<UUID, Avtaletype>> {
        @Language("PostgreSQL")
        val query = """
            select gjennomforing.id, avtale.avtaletype
            from gjennomforing
                join avtale on gjennomforing.avtale_id = avtale.id
            where (gjennomforing.start_dato <= :periode_slutt)
              and (gjennomforing.slutt_dato >= :periode_start or gjennomforing.slutt_dato is null)
              and (gjennomforing.avsluttet_tidspunkt > :periode_start or gjennomforing.avsluttet_tidspunkt is null)
              and not exists (
                    select 1
                    from utbetaling
                    where utbetaling.gjennomforing_id = gjennomforing.id
                      and utbetaling.periode && daterange(:periode_start, :periode_slutt)
              )
        """.trimIndent()

        val params = mapOf("periode_start" to periode.start, "periode_slutt" to periode.slutt)

        return session.list(queryOf(query, params)) {
            Pair(it.uuid("id"), Avtaletype.valueOf(it.string("avtaletype")))
        }
    }

    // TODO: returner årsak til hvorfor utbetaling ikke ble utført slik at dette kan assertes i tester
    private fun QueryContext.automatiskUtbetaling(utbetalingId: UUID): Boolean {
        val utbetaling = requireNotNull(queries.utbetaling.get(utbetalingId)) {
            "Fant ikke utbetaling med id=$utbetalingId"
        }
        when (utbetaling.beregning) {
            is UtbetalingBeregningFri -> {
                log.debug(
                    "Avbryter automatisk utbetaling. Prismodell {} er ikke egnet for automatisk utbetaling. UtbetalingId: {}",
                    utbetaling.beregning.javaClass,
                    utbetalingId,
                )
                return false
            }

            is UtbetalingBeregningForhandsgodkjent -> {}
        }
        val relevanteTilsagn = queries.tilsagn.getAll(
            gjennomforingId = utbetaling.gjennomforing.id,
            statuser = listOf(TilsagnStatus.GODKJENT),
            typer = listOf(TilsagnType.TILSAGN, TilsagnType.EKSTRATILSAGN),
            periodeIntersectsWith = utbetaling.periode,
        )
        if (relevanteTilsagn.size != 1) {
            log.debug(
                "Avbryter automatisk utbetaling. Feil antall tilsagn: {}. UtbetalingId: {}",
                relevanteTilsagn.size,
                utbetalingId,
            )
            return false
        }
        val tilsagn = relevanteTilsagn[0]
        if (tilsagn.belopGjenstaende < utbetaling.beregning.output.belop) {
            log.debug("Avbryter automatisk utbetaling. Ikke nok penger. UtbetalingId: {}", utbetalingId)
            return false
        }
        val gjorOppTilsagn = tilsagn.periode.getLastInclusiveDate() in utbetaling.periode
        val delutbetalingId = UUID.randomUUID()
        upsertDelutbetaling(
            utbetaling = utbetaling,
            tilsagn = tilsagn,
            id = delutbetalingId,
            belop = utbetaling.beregning.output.belop,
            gjorOppTilsagn = gjorOppTilsagn,
            behandletAv = Tiltaksadministrasjon,
        )
        val delutbetaling = requireNotNull(queries.delutbetaling.get(delutbetalingId))
        godkjennDelutbetaling(
            delutbetaling,
            Tiltaksadministrasjon,
        )
        log.debug("Automatisk behandling av utbetaling gjennomført. DelutbetalingId: {}", delutbetalingId)
        return true
    }

    private fun QueryContext.upsertDelutbetaling(
        utbetaling: Utbetaling,
        tilsagn: Tilsagn,
        id: UUID,
        belop: Int,
        gjorOppTilsagn: Boolean,
        behandletAv: Agent,
    ) {
        require(tilsagn.status == TilsagnStatus.GODKJENT) {
            "Tilsagn er ikke godkjent id=${tilsagn.id} status=${tilsagn.status}"
        }

        val periode = requireNotNull(utbetaling.periode.intersect(tilsagn.periode)) {
            "Utbetalingsperiode og tilsagnsperiode overlapper ikke"
        }

        val lopenummer = queries.delutbetaling.getNextLopenummerByTilsagn(tilsagn.id)
        val dbo = DelutbetalingDbo(
            id = id,
            utbetalingId = utbetaling.id,
            tilsagnId = tilsagn.id,
            status = DelutbetalingStatus.TIL_GODKJENNING,
            periode = periode,
            belop = belop,
            gjorOppTilsagn = gjorOppTilsagn,
            lopenummer = lopenummer,
            fakturanummer = fakturanummer(tilsagn.bestilling.bestillingsnummer, lopenummer),
            fakturaStatus = null,
        )

        queries.delutbetaling.upsert(dbo)
        queries.totrinnskontroll.upsert(
            Totrinnskontroll(
                id = UUID.randomUUID(),
                entityId = id,
                behandletAv = behandletAv,
                aarsaker = emptyList(),
                forklaring = null,
                type = Totrinnskontroll.Type.OPPRETT,
                behandletTidspunkt = LocalDateTime.now(),
                besluttelse = null,
                besluttetAv = null,
                besluttetTidspunkt = null,
            ),
        )
    }

    private fun QueryContext.godkjennDelutbetaling(
        delutbetaling: Delutbetaling,
        besluttetAv: Agent,
    ) {
        val opprettelse = queries.totrinnskontroll.getOrError(delutbetaling.id, Totrinnskontroll.Type.OPPRETT)
        require(opprettelse.besluttetAv == null) {
            "Utbetaling er allerede besluttet"
        }

        queries.delutbetaling.setStatus(delutbetaling.id, DelutbetalingStatus.GODKJENT)
        queries.totrinnskontroll.upsert(
            opprettelse.copy(
                besluttetAv = besluttetAv,
                besluttelse = Besluttelse.GODKJENT,
                besluttetTidspunkt = LocalDateTime.now(),
                aarsaker = emptyList(),
                forklaring = null,
            ),
        )
        val alleDelutbetalinger = queries.delutbetaling.getByUtbetalingId(delutbetaling.utbetalingId)
        if (alleDelutbetalinger.all { it.status == DelutbetalingStatus.GODKJENT }) {
            val utbetaling = requireNotNull(queries.utbetaling.get(delutbetaling.utbetalingId))
            queries.delutbetaling.setStatusForDelutbetalingerForBetaling(
                delutbetaling.utbetalingId,
                DelutbetalingStatus.OVERFORT_TIL_UTBETALING,
            )
            godkjennUtbetaling(utbetaling, alleDelutbetalinger)
        }
    }

    private fun QueryContext.godkjennUtbetaling(
        utbetaling: Utbetaling,
        delutbetalinger: List<Delutbetaling>,
    ) {
        require(delutbetalinger.isNotEmpty())
        delutbetalinger.forEach {
            val tilsagn = requireNotNull(queries.tilsagn.get(it.tilsagnId))
            if (tilsagn.status != TilsagnStatus.GODKJENT) {
                returnerDelutbetaling(
                    it,
                    listOf(AUTOMATISK_RETURNERT_AARSAK),
                    "Tilsagnet har ikke lenger status godkjent og kan derfor ikke benyttes for utbetaling",
                    Tiltaksadministrasjon,
                )
                return@godkjennUtbetaling
            }
            queries.tilsagn.setGjenstaendeBelop(tilsagn.id, tilsagn.belopGjenstaende - it.belop)
            if (it.gjorOppTilsagn) {
                tilsagnService.gjorOppAutomatisk(it.tilsagnId, this)
            }
            storeOpprettFaktura(it, tilsagn, utbetaling.betalingsinformasjon)
        }

        logEndring(
            "Overført til utbetaling",
            getOrError(delutbetalinger[0].utbetalingId),
            Tiltaksadministrasjon,
        )
    }

    private fun QueryContext.returnerDelutbetaling(
        delutbetaling: Delutbetaling,
        aarsaker: List<String>,
        forklaring: String?,
        besluttetAv: Agent,
    ) {
        setReturnertDelutbetaling(delutbetaling, aarsaker, forklaring, besluttetAv)

        // Set også de resterende delutbetalingene som returnert
        queries.delutbetaling.getByUtbetalingId(delutbetaling.utbetalingId)
            .filter { it.id != delutbetaling.id }
            .forEach {
                setReturnertDelutbetaling(
                    it,
                    listOf(AUTOMATISK_RETURNERT_AARSAK),
                    "Automatisk returnert av Nav Tiltaksadministrasjon som følge av at en annen utbetalingslinje ble returnert",
                    Tiltaksadministrasjon,
                )
            }

        logEndring(
            "Utbetaling returnert",
            getOrError(delutbetaling.utbetalingId),
            besluttetAv,
        )
    }

    private fun QueryContext.setReturnertDelutbetaling(
        delutbetaling: Delutbetaling,
        aarsaker: List<String>,
        forklaring: String?,
        besluttetAv: Agent,
    ) {
        val opprettelse = queries.totrinnskontroll.getOrError(delutbetaling.id, Totrinnskontroll.Type.OPPRETT)
        queries.delutbetaling.setStatus(delutbetaling.id, DelutbetalingStatus.RETURNERT)
        queries.totrinnskontroll.upsert(
            opprettelse.copy(
                besluttetAv = besluttetAv,
                besluttelse = Besluttelse.AVVIST,
                aarsaker = aarsaker,
                forklaring = forklaring,
                besluttetTidspunkt = LocalDateTime.now(),
            ),
        )
    }

    private fun resolveStengtHosArrangor(
        periode: Periode,
        stengtPerioder: List<GjennomforingDto.StengtPeriode>,
    ): Set<StengtPeriode> {
        return stengtPerioder
            .mapNotNull { stengt ->
                Periode.fromInclusiveDates(stengt.start, stengt.slutt).intersect(periode)?.let {
                    StengtPeriode(Periode(it.start, it.slutt), stengt.beskrivelse)
                }
            }
            .toSet()
    }

    private fun resolveDeltakelser(
        gjennomforingId: UUID,
        periode: Periode,
    ): Set<DeltakelsePerioder> = db.session {
        queries.deltaker.getAll(gjennomforingId = gjennomforingId)
            .asSequence()
            .filter { deltaker ->
                isRelevantForUtbetalingsperide(deltaker, periode)
            }
            .map { deltaker ->
                val deltakelsesmengder = queries.deltaker.getDeltakelsesmengder(deltaker.id)

                val sluttDatoInPeriode = getSluttDatoInPeriode(deltaker, periode)

                val perioder = deltakelsesmengder.mapIndexedNotNull { index, mengde ->
                    val gyldigTil = deltakelsesmengder.getOrNull(index + 1)?.gyldigFra ?: sluttDatoInPeriode

                    Periode.of(mengde.gyldigFra, gyldigTil)?.intersect(periode)?.let { overlappingPeriode ->
                        DeltakelsePeriode(
                            periode = overlappingPeriode,
                            deltakelsesprosent = mengde.deltakelsesprosent,
                        )
                    }
                }

                check(perioder.isNotEmpty()) {
                    "Deltaker id=${deltaker.id} er relevant for utbetaling, men mangler deltakelsesmengder innenfor perioden=$periode"
                }

                DeltakelsePerioder(deltaker.id, perioder)
            }
            .toSet()
    }

    private fun QueryContext.logEndring(
        operation: String,
        dto: Utbetaling,
        endretAv: Agent,
    ) {
        queries.endringshistorikk.logEndring(
            DocumentClass.UTBETALING,
            operation,
            endretAv,
            dto.id,
            LocalDateTime.now(),
        ) {
            Json.encodeToJsonElement(dto)
        }
    }

    private fun QueryContext.storeOpprettFaktura(
        delutbetaling: Delutbetaling,
        tilsagn: Tilsagn,
        betalingsinformasjon: Utbetaling.Betalingsinformasjon,
    ) {
        require(delutbetaling.status == DelutbetalingStatus.GODKJENT)

        val opprettelse = queries.totrinnskontroll.getOrError(delutbetaling.id, Totrinnskontroll.Type.OPPRETT)
        log.info("Sender delutbetaling med utbetalingId: ${delutbetaling.utbetalingId} tilsagnId: ${delutbetaling.tilsagnId} på kafka")

        val kontonummer = requireNotNull(betalingsinformasjon.kontonummer) {
            "Kontonummer mangler for utbetaling med id=${delutbetaling.utbetalingId}"
        }
        requireNotNull(opprettelse.besluttetTidspunkt)
        requireNotNull(opprettelse.besluttetAv)

        val beskrivelse = """
            Tiltakstype: ${tilsagn.tiltakstype.navn}
            Periode: ${tilsagn.periode.formatPeriode()}
            Tilsagnsnummer: ${tilsagn.bestilling.bestillingsnummer}
        """.trimIndent()

        val faktura = OpprettFaktura(
            fakturanummer = delutbetaling.faktura.fakturanummer,
            bestillingsnummer = tilsagn.bestilling.bestillingsnummer,
            betalingsinformasjon = OpprettFaktura.Betalingsinformasjon(
                kontonummer = kontonummer,
                kid = betalingsinformasjon.kid,
            ),
            belop = delutbetaling.belop,
            periode = delutbetaling.periode,
            behandletAv = opprettelse.behandletAv.toOkonomiPart(),
            behandletTidspunkt = opprettelse.behandletTidspunkt,
            besluttetAv = opprettelse.besluttetAv.toOkonomiPart(),
            besluttetTidspunkt = opprettelse.besluttetTidspunkt,
            gjorOppBestilling = delutbetaling.gjorOppTilsagn,
            beskrivelse = beskrivelse,
        )

        queries.delutbetaling.setSendtTilOkonomi(
            delutbetaling.utbetalingId,
            delutbetaling.tilsagnId,
            LocalDateTime.now(),
        )
        val message = OkonomiBestillingMelding.Faktura(faktura)
        storeOkonomiMelding(faktura.bestillingsnummer, message)
    }

    private fun QueryContext.storeOkonomiMelding(bestillingsnummer: String, message: OkonomiBestillingMelding) {
        val record = StoredProducerRecord(
            config.bestillingTopic,
            bestillingsnummer.toByteArray(),
            Json.encodeToString(message).toByteArray(),
            null,
        )
        queries.kafkaProducerRecord.storeRecord(record)
    }

    private fun QueryContext.getOrError(id: UUID): Utbetaling {
        return requireNotNull(queries.utbetaling.get(id)) { "Utbetaling med id=$id finnes ikke" }
    }
}

fun fakturanummer(bestillingsnummer: String, lopenummer: Int): String = "$bestillingsnummer-$lopenummer"

private fun isRelevantForUtbetalingsperide(
    deltaker: Deltaker,
    periode: Periode,
): Boolean {
    val relevantDeltakerStatusForUtbetaling = listOf(
        DeltakerStatus.Type.AVBRUTT,
        DeltakerStatus.Type.DELTAR,
        DeltakerStatus.Type.FULLFORT,
        DeltakerStatus.Type.HAR_SLUTTET,
    )
    if (deltaker.status.type !in relevantDeltakerStatusForUtbetaling) {
        return false
    }

    val startDato = requireNotNull(deltaker.startDato) {
        "Deltaker må ha en startdato når status er ${deltaker.status.type} og den er relevant for utbetaling"
    }
    val sluttDatoInPeriode = getSluttDatoInPeriode(deltaker, periode)
    return Periode.of(startDato, sluttDatoInPeriode)?.intersects(periode) ?: false
}

private fun getSluttDatoInPeriode(deltaker: Deltaker, periode: Periode): LocalDate {
    return deltaker.sluttDato?.plusDays(1)?.coerceAtMost(periode.slutt) ?: periode.slutt
}
