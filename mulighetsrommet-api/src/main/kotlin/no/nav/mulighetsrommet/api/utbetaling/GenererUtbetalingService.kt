package no.nav.mulighetsrommet.api.utbetaling

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.avtale.model.Prismodell
import no.nav.mulighetsrommet.api.clients.kontoregisterOrganisasjon.KontoregisterOrganisasjonClient
import no.nav.mulighetsrommet.api.endringshistorikk.DocumentClass
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingDto
import no.nav.mulighetsrommet.api.tilsagn.model.AvtalteSatser
import no.nav.mulighetsrommet.api.utbetaling.db.UtbetalingDbo
import no.nav.mulighetsrommet.api.utbetaling.model.*
import no.nav.mulighetsrommet.database.datatypes.toDaterange
import no.nav.mulighetsrommet.model.*
import no.nav.mulighetsrommet.utils.CacheUtils
import no.nav.tiltak.okonomi.Tilskuddstype
import org.intellij.lang.annotations.Language
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.TimeUnit

class GenererUtbetalingService(
    private val db: ApiDatabase,
    private val kontoregisterOrganisasjonClient: KontoregisterOrganisasjonClient,
) {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    private val kontonummerCache: Cache<String, Kontonummer> = Caffeine.newBuilder()
        .expireAfterWrite(30, TimeUnit.MINUTES)
        .maximumSize(10_000)
        .recordStats()
        .build()

    suspend fun genererUtbetalingForMonth(month: Int): List<Utbetaling> = db.transaction {
        val currentYear = LocalDate.now().year
        val date = LocalDate.of(currentYear, month, 1)
        val periode = Periode.forMonthOf(date)

        getGjennomforingerForGenereringAvUtbetalinger(periode)
            .mapNotNull { (gjennomforingId, prismodell) ->
                val gjennomforing = requireNotNull(queries.gjennomforing.get(gjennomforingId))
                val utbetaling = generateUtbetalingForPrismodell(UUID.randomUUID(), prismodell, gjennomforing, periode)
                utbetaling?.takeIf { isUtbetalingRelevantForArrangor(it) }
            }
            .map { utbetaling ->
                queries.utbetaling.upsert(utbetaling)
                val dto = getOrError(utbetaling.id)
                logEndring("Utbetaling opprettet", dto, Tiltaksadministrasjon)
                dto
            }
    }

    suspend fun oppdaterUtbetalingBeregningForGjennomforing(id: UUID): List<Utbetaling> = db.transaction {
        val gjennomforing = requireNotNull(queries.gjennomforing.get(id))
        val prismodell = requireNotNull(queries.avtale.get(gjennomforing.avtaleId!!)?.prismodell)

        queries.utbetaling
            .getByGjennomforing(id)
            .filter { it.innsender == null }
            .mapNotNull { utbetaling ->
                val oppdatertUtbetaling = when (utbetaling.beregning) {
                    is UtbetalingBeregningFri -> return@mapNotNull null

                    is UtbetalingBeregningPrisPerManedsverkMedDeltakelsesmengder,
                    is UtbetalingBeregningPrisPerManedsverk,
                    is UtbetalingBeregningPrisPerUkesverk,
                    -> generateUtbetalingForPrismodell(utbetaling.id, prismodell, gjennomforing, utbetaling.periode)
                }

                if (!isUtbetalingRelevantForArrangor(oppdatertUtbetaling)) {
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
        prismodell: Prismodell,
        gjennomforing: GjennomforingDto,
        periode: Periode,
    ): UtbetalingDbo? {
        val beregning = when (prismodell) {
            Prismodell.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK -> {
                val input = resolvePrisPerManedsverkMedDeltakelsesmengderInput(gjennomforing, periode)
                UtbetalingBeregningPrisPerManedsverkMedDeltakelsesmengder.beregn(input)
            }

            Prismodell.AVTALT_PRIS_PER_MANEDSVERK -> {
                val input = resolvePrisPerManedsverkInput(gjennomforing, periode)
                UtbetalingBeregningPrisPerManedsverk.beregn(input)
            }

            Prismodell.AVTALT_PRIS_PER_UKESVERK -> {
                val input = resolvePrisPerUkesverkInput(gjennomforing, periode)
                UtbetalingBeregningPrisPerUkesverk.beregn(input)
            }

            Prismodell.ANNEN_AVTALT_PRIS -> return null
        }

        if (beregning.output.belop == 0) {
            log.info(
                "Genererer ikke utbetaling for gjennomføring ${gjennomforing.id} i periode $periode, da beløpet er ${beregning.output.belop}",
            )
            return null
        }

        return createUtbetaling(
            utbetalingId = utbetalingId,
            gjennomforing = gjennomforing,
            periode = periode,
            beregning = beregning,
        )
    }

    private fun QueryContext.resolvePrisPerManedsverkMedDeltakelsesmengderInput(
        gjennomforing: GjennomforingDto,
        periode: Periode,
    ): UtbetalingBeregningPrisPerManedsverkMedDeltakelsesmengder.Input {
        val sats = resolveAvtaltSats(gjennomforing, periode)
        val stengtHosArrangor = resolveStengtHosArrangor(periode, gjennomforing.stengt)
        val deltakelser = resolveDeltakelserPerioderMedDeltakelsesmengder(gjennomforing.id, periode)
        return UtbetalingBeregningPrisPerManedsverkMedDeltakelsesmengder.Input(
            periode = periode,
            sats = sats,
            stengt = stengtHosArrangor,
            deltakelser = deltakelser,
        )
    }

    private fun QueryContext.resolvePrisPerManedsverkInput(
        gjennomforing: GjennomforingDto,
        periode: Periode,
    ): UtbetalingBeregningPrisPerManedsverk.Input {
        val sats = resolveAvtaltSats(gjennomforing, periode)
        val stengtHosArrangor = resolveStengtHosArrangor(periode, gjennomforing.stengt)
        val deltakelser = resolveDeltakelsePerioder(gjennomforing.id, periode)
        return UtbetalingBeregningPrisPerManedsverk.Input(
            periode = periode,
            sats = sats,
            stengt = stengtHosArrangor,
            deltakelser = deltakelser,
        )
    }

    private fun QueryContext.resolvePrisPerUkesverkInput(
        gjennomforing: GjennomforingDto,
        periode: Periode,
    ): UtbetalingBeregningPrisPerUkesverk.Input {
        val sats = resolveAvtaltSats(gjennomforing, periode)
        val stengtHosArrangor = resolveStengtHosArrangor(periode, gjennomforing.stengt)
        val deltakelser = resolveDeltakelsePerioder(gjennomforing.id, periode)
        return UtbetalingBeregningPrisPerUkesverk.Input(
            periode = periode,
            sats = sats,
            stengt = stengtHosArrangor,
            deltakelser = deltakelser,
        )
    }

    private suspend fun QueryContext.createUtbetaling(
        utbetalingId: UUID,
        gjennomforing: GjennomforingDto,
        periode: Periode,
        beregning: UtbetalingBeregning,
    ): UtbetalingDbo {
        val forrigeKrav = queries.utbetaling.getSisteGodkjenteUtbetaling(gjennomforing.id)
        val kontonummer = getKontonummer(gjennomforing.arrangor.organisasjonsnummer)
        return UtbetalingDbo(
            id = utbetalingId,
            gjennomforingId = gjennomforing.id,
            beregning = beregning,
            kontonummer = kontonummer,
            kid = forrigeKrav?.betalingsinformasjon?.kid,
            periode = periode,
            innsender = null,
            beskrivelse = null,
            tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
            godkjentAvArrangorTidspunkt = null,
        )
    }

    private fun QueryContext.resolveAvtaltSats(gjennomforing: GjennomforingDto, periode: Periode): Int {
        val avtale = requireNotNull(queries.avtale.get(gjennomforing.avtaleId!!))
        return AvtalteSatser.findSats(avtale, periode)
            ?: throw IllegalStateException("Sats mangler for periode $periode")
    }

    private suspend fun getKontonummer(organisasjonsnummer: Organisasjonsnummer): Kontonummer? {
        return CacheUtils.tryCacheFirstNullable(kontonummerCache, organisasjonsnummer.value) {
            kontoregisterOrganisasjonClient.getKontonummerForOrganisasjon(organisasjonsnummer).fold(
                { error ->
                    log.error(
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

    private fun QueryContext.getGjennomforingerForGenereringAvUtbetalinger(
        periode: Periode,
    ): List<Pair<UUID, Prismodell>> {
        @Language("PostgreSQL")
        val query = """
            select gjennomforing.id, avtale.prismodell
            from gjennomforing
                join avtale on gjennomforing.avtale_id = avtale.id
            where gjennomforing.status = 'GJENNOMFORES'
              and not exists (
                    select 1
                    from utbetaling
                    where utbetaling.gjennomforing_id = gjennomforing.id
                      and utbetaling.periode && ?::daterange
              )
        """.trimIndent()

        return session.list(queryOf(query, periode.toDaterange())) {
            Pair(it.uuid("id"), Prismodell.valueOf(it.string("prismodell")))
        }
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

    private fun resolveDeltakelserPerioderMedDeltakelsesmengder(
        gjennomforingId: UUID,
        periode: Periode,
    ): Set<DeltakelseDeltakelsesprosentPerioder> = db.session {
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
                        DeltakelsesprosentPeriode(
                            periode = overlappingPeriode,
                            deltakelsesprosent = mengde.deltakelsesprosent,
                        )
                    }
                }

                check(perioder.isNotEmpty()) {
                    "Deltaker id=${deltaker.id} er relevant for utbetaling, men mangler deltakelsesmengder innenfor perioden=$periode"
                }

                DeltakelseDeltakelsesprosentPerioder(deltaker.id, perioder)
            }
            .toSet()
    }

    private fun resolveDeltakelsePerioder(
        gjennomforingId: UUID,
        periode: Periode,
    ): Set<DeltakelsePeriode> = db.session {
        queries.deltaker.getAll(gjennomforingId = gjennomforingId)
            .asSequence()
            .filter { deltaker ->
                isRelevantForUtbetalingsperide(deltaker, periode)
            }
            .map { deltaker ->
                val sluttDatoInPeriode = getSluttDatoInPeriode(deltaker, periode)
                val overlappingPeriode = Periode(deltaker.startDato!!, sluttDatoInPeriode).intersect(periode)!!
                DeltakelsePeriode(deltaker.id, overlappingPeriode)
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

    private fun QueryContext.getOrError(id: UUID): Utbetaling {
        return queries.utbetaling.getOrError(id)
    }
}

fun isRelevantForUtbetalingsperide(
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

private fun isUtbetalingRelevantForArrangor(utbetaling: UtbetalingDbo?): Boolean {
    return utbetaling != null && utbetaling.beregning.output.belop > 0
}
