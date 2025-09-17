package no.nav.mulighetsrommet.api.utbetaling

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.OkonomiConfig
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.avtale.model.Prismodell
import no.nav.mulighetsrommet.api.clients.kontoregisterOrganisasjon.KontoregisterOrganisasjonClient
import no.nav.mulighetsrommet.api.endringshistorikk.DocumentClass
import no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing
import no.nav.mulighetsrommet.api.tilsagn.model.AvtalteSatser
import no.nav.mulighetsrommet.api.utbetaling.db.UtbetalingDbo
import no.nav.mulighetsrommet.api.utbetaling.mapper.UtbetalingMapper
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
    private val config: OkonomiConfig,
    private val db: ApiDatabase,
    private val kontoregisterOrganisasjonClient: KontoregisterOrganisasjonClient,
) {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    private data class UtbetalingContext(
        val gjennomforingId: UUID,
        val prismodell: Prismodell,
    )

    private val kontonummerCache: Cache<String, Kontonummer> = Caffeine.newBuilder()
        .expireAfterWrite(30, TimeUnit.MINUTES)
        .maximumSize(10_000)
        .recordStats()
        .build()

    suspend fun genererUtbetalingForPeriode(periode: Periode): List<Utbetaling> = db.transaction {
        getContextForGenereringAvUtbetalinger(periode)
            .mapNotNull { (gjennomforingId, prismodell) ->
                val gjennomforing = queries.gjennomforing.getOrError(gjennomforingId)
                generateUtbetalingForPrismodell(UUID.randomUUID(), prismodell, gjennomforing, periode)
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
            .mapNotNull { (gjennomforingId, prismodell) ->
                val gjennomforing = queries.gjennomforing.getOrError(gjennomforingId)
                val utbetaling = generateUtbetalingForPrismodell(
                    UUID.randomUUID(),
                    prismodell,
                    gjennomforing,
                    periode,
                )
                utbetaling?.let { UtbetalingMapper.toUtbetaling(it, gjennomforing) }
            }
    }

    suspend fun oppdaterUtbetalingBeregningForGjennomforing(id: UUID): List<Utbetaling> = db.transaction {
        val gjennomforing = queries.gjennomforing.get(id)
        val prismodell = queries.gjennomforing.getPrismodell(id)

        if (gjennomforing == null || prismodell == null) {
            log.warn("Klarte ikke utlede gjennomføring og/eller prismodell for id=$id")
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
                    -> false

                    UtbetalingStatusType.GENERERT -> true
                }
            }
            .mapNotNull { utbetaling ->
                val oppdatertUtbetaling = generateUtbetalingForPrismodell(
                    utbetaling.id,
                    prismodell,
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
        prismodell: Prismodell,
        gjennomforing: Gjennomforing,
        periode: Periode,
    ): UtbetalingDbo? {
        if (!isValidUtbetalingPeriode(gjennomforing.tiltakstype.tiltakskode, periode)) {
            return null
        }

        val beregning = when (prismodell) {
            Prismodell.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK -> {
                val input = resolveFastSatsPerTiltaksplassPerManedInput(gjennomforing, periode)
                UtbetalingBeregningFastSatsPerTiltaksplassPerManed.beregn(input)
            }

            Prismodell.AVTALT_PRIS_PER_MANEDSVERK -> {
                val input = resolvePrisPerManedsverkInput(gjennomforing, periode)
                UtbetalingBeregningPrisPerManedsverk.beregn(input)
            }

            Prismodell.AVTALT_PRIS_PER_UKESVERK -> {
                val input = resolvePrisPerUkesverkInput(gjennomforing, periode)
                UtbetalingBeregningPrisPerUkesverk.beregn(input)
            }

            Prismodell.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER,
            Prismodell.ANNEN_AVTALT_PRIS,
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
        gjennomforing: Gjennomforing,
        periode: Periode,
    ): UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Input {
        val sats = resolveAvtaltSats(gjennomforing, periode)
        val stengtHosArrangor = resolveStengtHosArrangor(periode, gjennomforing.stengt)
        val deltakelser = resolveDeltakelserPerioderMedDeltakelsesmengder(gjennomforing.id, periode)
        return UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Input(
            periode = periode,
            sats = sats,
            stengt = stengtHosArrangor,
            deltakelser = deltakelser,
        )
    }

    private fun QueryContext.resolvePrisPerManedsverkInput(
        gjennomforing: Gjennomforing,
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
        gjennomforing: Gjennomforing,
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
        gjennomforing: Gjennomforing,
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
            status = UtbetalingStatusType.GENERERT,
        )
    }

    private fun QueryContext.resolveAvtaltSats(gjennomforing: Gjennomforing, periode: Periode): Int {
        val avtale = requireNotNull(queries.avtale.get(gjennomforing.avtaleId!!))
        return AvtalteSatser.findSats(avtale, periode)
            ?: throw IllegalStateException("Klarte ikke utlede sats for gjennomføring=${gjennomforing.id} og periode=$periode")
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

    private fun QueryContext.getContextForGenereringAvUtbetalinger(
        periode: Periode,
    ): List<UtbetalingContext> {
        @Language("PostgreSQL")
        val query = """
            select gjennomforing.id, avtale.prismodell
            from gjennomforing
                join avtale on gjennomforing.avtale_id = avtale.id
            where gjennomforing.status != 'AVLYST'
              and daterange(gjennomforing.start_dato, coalesce(gjennomforing.avsluttet_tidspunkt::date, gjennomforing.slutt_dato), '[]') && :periode::daterange
              and not exists (
                    select 1
                    from utbetaling
                    where utbetaling.gjennomforing_id = gjennomforing.id
                      and utbetaling.periode && :periode::daterange
              )
        """.trimIndent()

        return session.list(queryOf(query, mapOf("periode" to periode.toDaterange()))) {
            UtbetalingContext(it.uuid("id"), Prismodell.valueOf(it.string("prismodell")))
        }
    }

    private fun QueryContext.getContextForBeregningAvUtbetalinger(
        periode: Periode,
    ): List<UtbetalingContext> {
        @Language("PostgreSQL")
        val query = """
            select gjennomforing.id, avtale.prismodell
            from gjennomforing
                join avtale on gjennomforing.avtale_id = avtale.id
            where gjennomforing.status != 'AVLYST'
              and daterange(gjennomforing.start_dato, coalesce(gjennomforing.avsluttet_tidspunkt::date, gjennomforing.slutt_dato), '[]') && :periode::daterange
        """.trimIndent()

        return session.list(queryOf(query, mapOf("periode" to periode.toDaterange()))) {
            UtbetalingContext(it.uuid("id"), Prismodell.valueOf(it.string("prismodell")))
        }
    }

    private fun isValidUtbetalingPeriode(tiltakskode: Tiltakskode, periode: Periode): Boolean {
        return config.gyldigTilsagnPeriode[tiltakskode]?.contains(periode) ?: false
    }

    private fun resolveStengtHosArrangor(
        periode: Periode,
        stengtPerioder: List<Gjennomforing.StengtPeriode>,
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
            .mapNotNull { deltaker ->
                toDeltakelsePeriode(deltaker, periode)
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
        return resolveDeltakelsePerioder(deltakere, periode)
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

private fun resolveDeltakelsePerioder(
    deltakere: List<Deltaker>,
    periode: Periode,
): Set<DeltakelsePeriode> {
    return deltakere
        .asSequence()
        .mapNotNull { deltaker ->
            toDeltakelsePeriode(deltaker, periode)
        }
        .toSet()
}

private fun toDeltakelsePeriode(
    deltaker: Deltaker,
    periode: Periode,
): DeltakelsePeriode? {
    if (!harDeltakerDeltatt(deltaker)) {
        return null
    }

    val startDato = requireNotNull(deltaker.startDato) {
        "Deltaker må ha en startdato når status er ${deltaker.status.type} og den er relevant for utbetaling"
    }
    val sluttDatoInPeriode = getSluttDatoInPeriode(deltaker, periode)
    val overlappingPeriode = Periode.of(startDato, sluttDatoInPeriode)?.intersect(periode) ?: return null
    return DeltakelsePeriode(deltaker.id, overlappingPeriode)
}

private fun harDeltakerDeltatt(deltaker: Deltaker): Boolean {
    if (deltaker.status.type == DeltakerStatusType.DELTAR) {
        return true
    }

    val avsluttendeStatus = listOf(
        DeltakerStatusType.AVBRUTT,
        DeltakerStatusType.FULLFORT,
        DeltakerStatusType.HAR_SLUTTET,
    )
    return deltaker.status.type in avsluttendeStatus && deltaker.sluttDato != null
}

private fun getSluttDatoInPeriode(deltaker: Deltaker, periode: Periode): LocalDate {
    return deltaker.sluttDato?.plusDays(1)?.coerceAtMost(periode.slutt) ?: periode.slutt
}
