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
import no.nav.mulighetsrommet.api.utbetaling.model.SystemgenerertPrismodell
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregning
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
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.TimeUnit

class GenererUtbetalingService(
    private val config: Config,
    private val db: ApiDatabase,
    private val prismodeller: Set<SystemgenerertPrismodell<*, *>>,
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
                    periode = context.periode,
                )
                utbetaling?.let { UtbetalingMapper.toUtbetaling(it, gjennomforing) }
            }
    }

    suspend fun oppdaterUtbetalingBeregningForGjennomforing(id: UUID): List<Utbetaling> = db.transaction {
        val gjennomforing = queries.gjennomforing.getGruppetiltakOrError(id)

        if (gjennomforing.prismodell == null) {
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
                    utbetalingId = utbetaling.id,
                    gjennomforing = gjennomforing,
                    periode = utbetaling.periode,
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

    suspend fun regenererUtbetaling(utbetaling: Utbetaling): Utbetaling = db.transaction {
        val gjennomforing = queries.gjennomforing.getGruppetiltakOrError(utbetaling.gjennomforing.id)

        val utbetalingerSammePeriode = queries.utbetaling.getByGjennomforing(gjennomforing.id)
            .filter { it.periode == utbetaling.periode }

        val alleredeRegenerert = utbetalingerSammePeriode
            .sortedByDescending { it.createdAt }
            .firstOrNull { it.status != UtbetalingStatusType.AVBRUTT }

        if (alleredeRegenerert != null) {
            throw IllegalArgumentException("Allerede regenerert med id=${alleredeRegenerert.id}")
        }

        val nyUtbetaling = generateUtbetalingForPrismodell(
            utbetalingId = UUID.randomUUID(),
            gjennomforing = gjennomforing,
            periode = utbetaling.periode,
        )

        if (nyUtbetaling == null) {
            throw IllegalArgumentException("Generert utbetaling var null utbetaling=${utbetaling.id} fordi den ikke lengre er relevant for arrangør")
        }

        queries.utbetaling.upsert(nyUtbetaling)
        val dto = getOrError(nyUtbetaling.id)
        logEndring("Utbetaling opprettet", dto, Tiltaksadministrasjon)
        dto
    }

    private suspend fun QueryContext.generateUtbetalingForPrismodell(
        utbetalingId: UUID,
        gjennomforing: GjennomforingGruppetiltak,
        periode: Periode,
    ): UtbetalingDbo? {
        if (!isValidUtbetalingPeriode(gjennomforing.tiltakstype.tiltakskode, periode)) {
            log.info("Genererer ikke utbetaling for gjennomforing=${gjennomforing.id} fordi utbetalingsperioden ikke er tillatt tiltakskode=${gjennomforing.tiltakstype.tiltakskode}, periode=$periode")
            return null
        }

        val type = gjennomforing.prismodell?.type
            ?: throw IllegalStateException("Gjennomføring med id=${gjennomforing.id} mangler prismodell")

        val prismodell = prismodeller.singleOrNull { it.genereringContext(periode).prismodellType == type } ?: run {
            log.info("Genererer ikke utbetaling for gjennomføring=${gjennomforing.id} fordi prismodellen ikke er støttet type=$type")
            return null
        }

        return prismodell.calculate(gjennomforing, periode).takeIf { it.output.belop > 0 }?.let {
            createUtbetaling(
                utbetalingId = utbetalingId,
                gjennomforing = gjennomforing,
                periode = periode,
                beregning = it,
            )
        }
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
        return prismodeller
            .map { it.genereringContext(periode) }
            .flatMap { context ->
                getContextForPrismodellCommon(
                    context.prismodellType,
                    context.tilskuddstype,
                    context.periode,
                    includeNotExists,
                )
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
                join prismodell on prismodell.id = gjennomforing.prismodell_id
            where gjennomforing.status != 'AVLYST'
                and prismodell.prismodell_type = :prismodell::prismodell_type
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
