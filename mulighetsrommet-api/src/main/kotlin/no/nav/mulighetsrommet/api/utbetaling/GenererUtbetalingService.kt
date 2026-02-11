package no.nav.mulighetsrommet.api.utbetaling

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.arrangor.model.Betalingsinformasjon
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
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import no.nav.mulighetsrommet.tasks.executeSuspend
import no.nav.mulighetsrommet.tasks.transactionalSchedulerClient
import no.nav.mulighetsrommet.utils.CacheUtils
import no.nav.tiltak.okonomi.Tilskuddstype
import org.intellij.lang.annotations.Language
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.TimeUnit

class GenererUtbetalingService(
    private val config: Config,
    private val db: ApiDatabase,
    private val prismodeller: Set<SystemgenerertPrismodell<*>>,
    private val kontoregisterOrganisasjonClient: KontoregisterOrganisasjonClient,
) {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    class Config(
        val gyldigTilsagnPeriode: Map<Tiltakskode, Periode>,
        val tidligstTidspunktForUtbetaling: TidligstTidspunktForUtbetalingCalculator,
    )

    @Serializable
    data class OppdaterUtbetalingerTaskData(
        @Serializable(with = UUIDSerializer::class)
        val gjennomforingId: UUID,
    )

    val task: OneTimeTask<OppdaterUtbetalingerTaskData> = Tasks
        .oneTime(javaClass.simpleName, OppdaterUtbetalingerTaskData::class.java)
        .executeSuspend { instance, _ ->
            oppdaterUtbetalingerForGjennomforing(instance.data.gjennomforingId)
        }

    private data class UtbetalingContext(
        val gjennomforingId: UUID,
        val periode: Periode,
    )

    private val kontonummerCache: Cache<String, Kontonummer> = Caffeine.newBuilder()
        .expireAfterWrite(30, TimeUnit.MINUTES)
        .maximumSize(10_000)
        .recordStats()
        .build()

    suspend fun genererUtbetalingerForPeriode(periode: Periode): List<Utbetaling> = db.transaction {
        getContextForGenereringAvUtbetalinger(periode)
            .mapNotNull { context ->
                val gjennomforing = queries.gjennomforing.getGruppetiltakOrError(context.gjennomforingId)
                genererUtbetaling(
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
                val utbetaling = genererUtbetaling(
                    utbetalingId = UUID.randomUUID(),
                    gjennomforing = gjennomforing,
                    periode = context.periode,
                )
                utbetaling?.let { UtbetalingMapper.toNewUtbetaling(it, gjennomforing) }
            }
    }

    fun skedulerOppdaterUtbetalingerForGjennomforing(gjennomforingId: UUID, tidspunkt: Instant): Unit = db.transaction {
        if (hentGenererteUtbetalinger(gjennomforingId).isEmpty()) {
            return
        }

        val instance = task.instance(gjennomforingId.toString(), OppdaterUtbetalingerTaskData(gjennomforingId))
        val client = transactionalSchedulerClient(task, session.connection.underlying)
        client.scheduleIfNotExists(instance, tidspunkt)
    }

    // TODO: vurdere om denne burde utbedres til å fange opp at deltakere har blitt påmeldt med bakovervirkende
    //  kraft _etter_ månedens kjøring av "generer utbetalinger".
    //  I disse tilfellene vil ikke systemet fange opp at utbetlinagen _burde_ blitt generert etterskuddsvis.
    //  Hvis vi skal støtte dette så må vi sørge for det ikke genereres opp utbetalinger lengre tilbake i tid enn f.eks.
    //  gyldige tilsagnsperioder.
    suspend fun oppdaterUtbetalingerForGjennomforing(gjennomforingId: UUID): List<Utbetaling> = db.transaction {
        val gjennomforing = queries.gjennomforing.getGruppetiltakOrError(gjennomforingId)

        if (gjennomforing.prismodell == null) {
            log.info("Prismodell er ikke satt for gjennomføring med id=$gjennomforingId")
            return listOf()
        }

        hentGenererteUtbetalinger(gjennomforing.id)
            .mapNotNull { utbetaling ->
                val oppdatertUtbetaling = genererUtbetaling(
                    utbetalingId = utbetaling.id,
                    gjennomforing = gjennomforing,
                    periode = utbetaling.periode,
                )

                if (oppdatertUtbetaling == null) {
                    log.info("Sletter utbetaling=${utbetaling.id} fordi den ikke lengre er relevant for arrangør")
                    queries.utbetaling.delete(utbetaling.id)
                    return@mapNotNull null
                }

                oppdatertUtbetaling.takeIf { it.isNotEqualTo(utbetaling) }
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

        val nyUtbetaling = genererUtbetaling(
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

    private fun QueryContext.hentGenererteUtbetalinger(gjennomforingId: UUID): List<Utbetaling> {
        return queries.utbetaling.getByGjennomforing(gjennomforingId).filter {
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
    }

    private suspend fun QueryContext.genererUtbetaling(
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

        val prismodell = prismodeller.singleOrNull { it.type == type } ?: run {
            log.info("Genererer ikke utbetaling for gjennomføring=${gjennomforing.id} fordi prismodellen ikke er støttet type=$type")
            return null
        }

        val deltakere = queries.deltaker.getByGjennomforingId(gjennomforing.id)

        return prismodell.beregn(gjennomforing, deltakere, periode).takeIf { it.output.pris.belop > 0 }?.let {
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
        val forrigeKid = when (forrigeKrav?.betalingsinformasjon) {
            is Betalingsinformasjon.BBan -> forrigeKrav.betalingsinformasjon.kid
            else -> null
        }
        return UtbetalingDbo(
            id = utbetalingId,
            gjennomforingId = gjennomforing.id,
            status = UtbetalingStatusType.GENERERT,
            valuta = beregning.output.pris.valuta,
            beregning = beregning,
            betalingsinformasjon = kontonummer?.let {
                Betalingsinformasjon.BBan(
                    kontonummer = it,
                    kid = forrigeKid,
                )
            },
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
        return prismodeller.flatMap { prismodell ->
            getContextForPrismodell(
                prismodell.type,
                prismodell.tilskuddstype,
                prismodell.justerPeriodeForBeregning(periode),
                includeNotExists,
            )
        }
    }

    private fun QueryContext.getContextForPrismodell(
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
            UtbetalingContext(it.uuid("id"), periode)
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

private fun UtbetalingDbo.isNotEqualTo(utbetaling: Utbetaling): Boolean = this != UtbetalingDbo(
    id = utbetaling.id,
    innsender = utbetaling.innsender,
    gjennomforingId = utbetaling.gjennomforing.id,
    status = utbetaling.status,
    valuta = utbetaling.valuta,
    beregning = utbetaling.beregning,
    betalingsinformasjon = utbetaling.betalingsinformasjon,
    periode = utbetaling.periode,
    beskrivelse = utbetaling.beskrivelse,
    tilskuddstype = utbetaling.tilskuddstype,
    godkjentAvArrangorTidspunkt = utbetaling.godkjentAvArrangorTidspunkt,
    utbetalesTidligstTidspunkt = utbetaling.utbetalesTidligstTidspunkt,
)
