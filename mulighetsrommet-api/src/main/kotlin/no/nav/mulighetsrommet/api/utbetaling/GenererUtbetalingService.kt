package no.nav.mulighetsrommet.api.utbetaling

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
import no.nav.mulighetsrommet.api.clients.kontoregisterOrganisasjon.KontoregisterOrganisasjonClient
import no.nav.mulighetsrommet.api.endringshistorikk.DocumentClass
import no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingGruppetiltak
import no.nav.mulighetsrommet.api.utbetaling.db.UtbetalingDbo
import no.nav.mulighetsrommet.api.utbetaling.mapper.UtbetalingMapper
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakelseDeltakelsesprosentPerioder
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakelsePeriode
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakelsesprosentPeriode
import no.nav.mulighetsrommet.api.utbetaling.model.StengtPeriode
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregning
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFastSatsPerTiltaksplassPerManed
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningPrisPerHeleUkesverk
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningPrisPerManedsverk
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningPrisPerUkesverk
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType
import no.nav.mulighetsrommet.database.datatypes.toDaterange
import no.nav.mulighetsrommet.model.Agent
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltaksadministrasjon
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.utils.CacheUtils
import no.nav.tiltak.okonomi.Tilskuddstype
import org.intellij.lang.annotations.Language
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters
import java.util.UUID
import java.util.concurrent.TimeUnit

class GenererUtbetalingService(
    private val config: Config,
    private val db: ApiDatabase,
    private val kontoregisterOrganisasjonClient: KontoregisterOrganisasjonClient,
) {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    class Config(
        val gyldigTilsagnPeriode: Map<Tiltakskode, Periode>,
        val tidligstTidspunktForUtbetaling: TidligstTidspunktForUtbetalingCalculator,
    )

    private data class UtbetalingContext(
        val gjennomforingId: UUID,
        val prismodell: PrismodellType,
        val periode: Periode,
    )

    private val kontonummerCache: Cache<String, Kontonummer> = Caffeine.newBuilder()
        .expireAfterWrite(30, TimeUnit.MINUTES)
        .maximumSize(10_000)
        .recordStats()
        .build()

    suspend fun genererUtbetalingForPeriode(periode: Periode): List<Utbetaling> = db.transaction {
        getContextForGenereringAvUtbetalinger(periode)
            .mapNotNull { context ->
                val gjennomforing = queries.gjennomforing.getGruppetiltakOrError(context.gjennomforingId)
                generateUtbetalingForPrismodell(
                    utbetalingId = UUID.randomUUID(),
                    gjennomforing = gjennomforing,
                    prismodell = context.prismodell,
                    periode = context.periode,
                )
            }
            .map { utbetaling ->
                queries.utbetaling.upsert(utbetaling)
                val dto = getOrError(utbetaling.id)
                logEndring("Utbetaling opprettet", dto, Tiltaksadministrasjon)
                dto
            }
    }

    suspend fun beregnUtbetalingerForPeriode(periode: Periode): List<Utbetaling> = db.transaction {
        getContextForBeregningAvUtbetalinger(periode)
            .mapNotNull { context ->
                val gjennomforing = queries.gjennomforing.getGruppetiltakOrError(context.gjennomforingId)
                val utbetaling = generateUtbetalingForPrismodell(
                    utbetalingId = UUID.randomUUID(),
                    gjennomforing = gjennomforing,
                    prismodell = context.prismodell,
                    periode = context.periode,
                )
                utbetaling?.let { UtbetalingMapper.toUtbetaling(it, gjennomforing) }
            }
    }

    suspend fun oppdaterUtbetalingBeregningForGjennomforing(id: UUID): List<Utbetaling> = db.transaction {
        val gjennomforing = queries.gjennomforing.getGruppetiltakOrError(id)
        val prismodell = queries.gjennomforing.getPrismodell(id)

        if (prismodell == null) {
            log.info("Prismodell er ikke satt for gjennomføring med id=$id")
            return listOf()
        }

        queries.utbetaling
            .getByGjennomforing(id)
            .filter {
                when (it.status) {
                    UtbetalingStatusType.INNSENDT,
                    UtbetalingStatusType.TIL_ATTESTERING,
                    UtbetalingStatusType.RETURNERT,
                    UtbetalingStatusType.FERDIG_BEHANDLET,
                    UtbetalingStatusType.DELVIS_UTBETALT,
                    UtbetalingStatusType.UTBETALT,
                    UtbetalingStatusType.AVBRUTT,
                    -> false

                    UtbetalingStatusType.GENERERT -> true
                }
            }
            .mapNotNull { utbetaling ->
                val oppdatertUtbetaling = generateUtbetalingForPrismodell(
                    utbetaling.id,
                    prismodell.type,
                    gjennomforing,
                    utbetaling.periode,
                )

                if (oppdatertUtbetaling == null) {
                    log.info("Sletter utbetaling=${utbetaling.id} fordi den ikke lengre er relevant for arrangør")
                    queries.utbetaling.delete(utbetaling.id)
                    return@mapNotNull null
                }

                oppdatertUtbetaling
            }
            .map { utbetaling ->
                queries.utbetaling.upsert(utbetaling)
                val dto = getOrError(utbetaling.id)
                logEndring("Utbetaling beregning oppdatert", dto, Tiltaksadministrasjon)
                dto
            }
    }

    private suspend fun QueryContext.generateUtbetalingForPrismodell(
        utbetalingId: UUID,
        prismodell: PrismodellType,
        gjennomforing: GjennomforingGruppetiltak,
        periode: Periode,
    ): UtbetalingDbo? {
        if (!isValidUtbetalingPeriode(gjennomforing.tiltakstype.tiltakskode, periode)) {
            log.info("Genererer ikke utbetaling for gjennomforing=${gjennomforing.id} fordi utbetalingsperioden ikke er tillatt tiltakskode=${gjennomforing.tiltakstype.tiltakskode}, periode=$periode")
            return null
        }

        val beregning = when (prismodell) {
            PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK -> {
                val input = resolveFastSatsPerTiltaksplassPerManedInput(gjennomforing, periode)
                UtbetalingBeregningFastSatsPerTiltaksplassPerManed.beregn(input)
            }

            PrismodellType.AVTALT_PRIS_PER_MANEDSVERK -> {
                val input = resolvePrisPerManedsverkInput(gjennomforing, periode)
                UtbetalingBeregningPrisPerManedsverk.beregn(input)
            }

            PrismodellType.AVTALT_PRIS_PER_UKESVERK -> {
                val input = resolvePrisPerUkesverkInput(gjennomforing, periode)
                UtbetalingBeregningPrisPerUkesverk.beregn(input)
            }

            PrismodellType.AVTALT_PRIS_PER_HELE_UKESVERK -> {
                val input = resolvePrisPerHeleUkesverkInput(gjennomforing, periode)
                UtbetalingBeregningPrisPerHeleUkesverk.beregn(input)
            }

            PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER,
            PrismodellType.ANNEN_AVTALT_PRIS,
            -> return null
        }

        return beregning.takeIf { it.output.belop > 0 }?.let {
            createUtbetaling(
                utbetalingId = utbetalingId,
                gjennomforing = gjennomforing,
                periode = periode,
                beregning = it,
            )
        }
    }

    private fun QueryContext.resolveFastSatsPerTiltaksplassPerManedInput(
        gjennomforing: GjennomforingGruppetiltak,
        periode: Periode,
    ): UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Input {
        val satser = UtbetalingInputHelper.resolveAvtalteSatser(gjennomforing, periode)
        val stengtHosArrangor = resolveStengtHosArrangor(periode, gjennomforing.stengt)
        val deltakelser = resolveDeltakelserPerioderMedDeltakelsesmengder(gjennomforing.id, periode)
        return UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Input(
            satser = satser,
            stengt = stengtHosArrangor,
            deltakelser = deltakelser,
        )
    }

    private fun QueryContext.resolvePrisPerManedsverkInput(
        gjennomforing: GjennomforingGruppetiltak,
        periode: Periode,
    ): UtbetalingBeregningPrisPerManedsverk.Input {
        val satser = UtbetalingInputHelper.resolveAvtalteSatser(gjennomforing, periode)
        val stengtHosArrangor = resolveStengtHosArrangor(periode, gjennomforing.stengt)
        val deltakelser = resolveDeltakelsePerioder(gjennomforing.id, periode)
        return UtbetalingBeregningPrisPerManedsverk.Input(
            satser = satser,
            stengt = stengtHosArrangor,
            deltakelser = deltakelser,
        )
    }

    private fun QueryContext.resolvePrisPerUkesverkInput(
        gjennomforing: GjennomforingGruppetiltak,
        periode: Periode,
    ): UtbetalingBeregningPrisPerUkesverk.Input {
        val satser = UtbetalingInputHelper.resolveAvtalteSatser(gjennomforing, periode)
        val stengtHosArrangor = resolveStengtHosArrangor(periode, gjennomforing.stengt)
        val deltakelser = resolveDeltakelsePerioder(gjennomforing.id, periode)
        return UtbetalingBeregningPrisPerUkesverk.Input(
            satser = satser,
            stengt = stengtHosArrangor,
            deltakelser = deltakelser,
        )
    }

    private fun QueryContext.resolvePrisPerHeleUkesverkInput(
        gjennomforing: GjennomforingGruppetiltak,
        periode: Periode,
    ): UtbetalingBeregningPrisPerHeleUkesverk.Input {
        val satser = UtbetalingInputHelper.resolveAvtalteSatser(gjennomforing, periode)
        val stengtHosArrangor = resolveStengtHosArrangor(periode, gjennomforing.stengt)
        val deltakelser = resolveDeltakelsePerioder(gjennomforing.id, periode)
        return UtbetalingBeregningPrisPerHeleUkesverk.Input(
            satser = satser,
            stengt = stengtHosArrangor,
            deltakelser = deltakelser,
        )
    }

    private suspend fun QueryContext.createUtbetaling(
        utbetalingId: UUID,
        gjennomforing: Gjennomforing,
        periode: Periode,
        beregning: UtbetalingBeregning,
    ): UtbetalingDbo {
        val forrigeKrav = queries.utbetaling.getSisteGodkjenteUtbetaling(gjennomforing.id)
        val kontonummer = getKontonummer(gjennomforing.arrangor.organisasjonsnummer)
        val utbetalesTidligstTidspunkt = config.tidligstTidspunktForUtbetaling.calculate(
            gjennomforing.tiltakstype.tiltakskode,
            periode,
        )
        return UtbetalingDbo(
            id = utbetalingId,
            gjennomforingId = gjennomforing.id,
            status = UtbetalingStatusType.GENERERT,
            beregning = beregning,
            kontonummer = kontonummer,
            kid = forrigeKrav?.betalingsinformasjon?.kid,
            periode = periode,
            innsender = null,
            beskrivelse = null,
            tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
            godkjentAvArrangorTidspunkt = null,
            utbetalesTidligstTidspunkt = utbetalesTidligstTidspunkt,
        )
    }

    private suspend fun getKontonummer(organisasjonsnummer: Organisasjonsnummer): Kontonummer? {
        return CacheUtils.tryCacheFirstNullable(kontonummerCache, organisasjonsnummer.value) {
            kontoregisterOrganisasjonClient.getKontonummerForOrganisasjon(organisasjonsnummer).fold(
                { error ->
                    log.warn(
                        "Kunne ikke hente kontonummer for organisasjon ${organisasjonsnummer.value}. Error: $error",
                    )
                    null
                },
                { response ->
                    Kontonummer(response.kontonr)
                },
            )
        }
    }

    private fun QueryContext.getContextForGenereringAvUtbetalinger(periode: Periode): List<UtbetalingContext> {
        return getContextForUtbetalinger(periode, includeNotExists = true)
    }

    private fun QueryContext.getContextForBeregningAvUtbetalinger(periode: Periode): List<UtbetalingContext> {
        return getContextForUtbetalinger(periode, includeNotExists = false)
    }

    private fun QueryContext.getContextForUtbetalinger(
        periode: Periode,
        includeNotExists: Boolean,
    ): List<UtbetalingContext> {
        val systemgenerertePrismodeller = listOf(
            Triple(PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK, Tilskuddstype.TILTAK_DRIFTSTILSKUDD, periode),
            Triple(PrismodellType.AVTALT_PRIS_PER_MANEDSVERK, Tilskuddstype.TILTAK_DRIFTSTILSKUDD, periode),
            Triple(PrismodellType.AVTALT_PRIS_PER_UKESVERK, Tilskuddstype.TILTAK_DRIFTSTILSKUDD, periode),
            Triple(
                PrismodellType.AVTALT_PRIS_PER_HELE_UKESVERK,
                Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
                heleUkerPeriode(periode),
            ),
        )
        return systemgenerertePrismodeller.flatMap { (prismodell, tilskuddstype, justertPeriode) ->
            getContextForPrismodellCommon(prismodell, tilskuddstype, justertPeriode, includeNotExists)
        }
    }

    private fun QueryContext.getContextForPrismodellCommon(
        prismodell: PrismodellType,
        tilskuddstype: Tilskuddstype,
        periode: Periode,
        includeNotExists: Boolean,
    ): List<UtbetalingContext> {
        val notExistsClause = """
            and not exists (
                select 1
                from utbetaling
                where utbetaling.gjennomforing_id = gjennomforing.id
                  and utbetaling.periode && :periode::daterange
                  and utbetaling.tilskuddstype = :tilskuddstype::tilskuddstype
            )
        """.takeIf { includeNotExists }.orEmpty()

        @Language("PostgreSQL")
        val query = """
            select gjennomforing.id
            from gjennomforing
                join avtale_prismodell on avtale_prismodell.avtale_id = gjennomforing.avtale_id
            where gjennomforing.status != 'AVLYST'
                and avtale_prismodell.prismodell_type = :prismodell::prismodell
                and daterange(gjennomforing.start_dato, coalesce(gjennomforing.avsluttet_tidspunkt::date, gjennomforing.slutt_dato), '[]') && :periode::daterange
                $notExistsClause
        """.trimIndent()

        val params = mapOf(
            "prismodell" to prismodell.name,
            "periode" to periode.toDaterange(),
            "tilskuddstype" to tilskuddstype.name,
        )

        return session.list(queryOf(query, params)) {
            UtbetalingContext(it.uuid("id"), prismodell, periode)
        }
    }

    private fun isValidUtbetalingPeriode(tiltakskode: Tiltakskode, periode: Periode): Boolean {
        return config.gyldigTilsagnPeriode[tiltakskode]?.contains(periode) ?: false
    }

    private fun resolveStengtHosArrangor(
        periode: Periode,
        stengtPerioder: List<GjennomforingGruppetiltak.StengtPeriode>,
    ): Set<StengtPeriode> {
        return stengtPerioder
            .mapNotNull { stengt ->
                Periode.fromInclusiveDates(stengt.start, stengt.slutt).intersect(periode)?.let {
                    StengtPeriode(Periode(it.start, it.slutt), stengt.beskrivelse)
                }
            }
            .toSet()
    }

    private fun QueryContext.resolveDeltakelserPerioderMedDeltakelsesmengder(
        gjennomforingId: UUID,
        periode: Periode,
    ): Set<DeltakelseDeltakelsesprosentPerioder> {
        return queries.deltaker.getAll(gjennomforingId = gjennomforingId)
            .asSequence()
            .mapNotNull { deltaker ->
                UtbetalingInputHelper.toDeltakelsePeriode(deltaker, periode)
            }
            .map { (deltakelseId, deltakelsePeriode) ->
                val deltakelsesmengder = queries.deltaker.getDeltakelsesmengder(deltakelseId)

                val perioder = deltakelsesmengder.mapIndexedNotNull { index, mengde ->
                    val gyldigTil = deltakelsesmengder.getOrNull(index + 1)?.gyldigFra ?: deltakelsePeriode.slutt

                    Periode.of(mengde.gyldigFra, gyldigTil)?.intersect(periode)?.let { overlappingPeriode ->
                        DeltakelsesprosentPeriode(
                            periode = overlappingPeriode,
                            deltakelsesprosent = mengde.deltakelsesprosent,
                        )
                    }
                }

                check(perioder.isNotEmpty()) {
                    "Deltaker id=$deltakelseId er relevant for utbetaling, men mangler deltakelsesmengder innenfor perioden=$periode"
                }

                DeltakelseDeltakelsesprosentPerioder(deltakelseId, perioder)
            }
            .toSet()
    }

    private fun QueryContext.resolveDeltakelsePerioder(
        gjennomforingId: UUID,
        periode: Periode,
    ): Set<DeltakelsePeriode> {
        val deltakere = queries.deltaker.getAll(gjennomforingId = gjennomforingId)
        return UtbetalingInputHelper.resolveDeltakelsePerioder(deltakere, periode)
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

    private fun QueryContext.getOrError(id: UUID): Utbetaling {
        return queries.utbetaling.getOrError(id)
    }
}

fun heleUkerPeriode(periode: Periode): Periode {
    val newStart = if (periode.start.dayOfWeek <= DayOfWeek.WEDNESDAY) {
        periode.start.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    } else {
        periode.start.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY))
    }
    val newSlutt = if (periode.slutt.dayOfWeek >= DayOfWeek.THURSDAY) {
        periode.slutt.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY))
    } else {
        periode.slutt.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }
    return Periode(newStart, newSlutt)
}
