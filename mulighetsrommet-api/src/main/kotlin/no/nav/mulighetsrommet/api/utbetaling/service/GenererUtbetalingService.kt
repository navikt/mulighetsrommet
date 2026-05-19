package no.nav.mulighetsrommet.api.utbetaling.service

import arrow.core.getOrElse
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import kotlinx.serialization.Serializable
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.arrangor.model.Betalingsinformasjon
import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingAvtale
import no.nav.mulighetsrommet.api.utbetaling.db.DeltakerForslag
import no.nav.mulighetsrommet.api.utbetaling.mapper.UtbetalingMapper
import no.nav.mulighetsrommet.api.utbetaling.model.SystemgenerertPrismodell
import no.nav.mulighetsrommet.api.utbetaling.model.UpsertUtbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingAdvarsler
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregning
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingException
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType
import no.nav.mulighetsrommet.database.datatypes.toDaterange
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltaksadministrasjon
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import no.nav.mulighetsrommet.tasks.executeSuspend
import no.nav.mulighetsrommet.tasks.transactionalSchedulerClient
import no.nav.tiltak.okonomi.Tilskuddstype
import org.intellij.lang.annotations.Language
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

class GenererUtbetalingService(
    private val config: Config,
    private val db: ApiDatabase,
    private val utbetalingService: UtbetalingService,
    private val prismodeller: Set<SystemgenerertPrismodell<*>>,
) {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    class Config(
        val gyldigTilsagnPeriode: Map<Tiltakskode, Periode>,
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
        val gjennomforing: GjennomforingAvtale,
        val periode: Periode,
    )

    suspend fun genererUtbetalingerForPeriode(periode: Periode): List<Utbetaling> {
        return getContextForGenereringAvUtbetalinger(periode).mapNotNull { context ->
            val beregning = beregnUtbetaling(context.gjennomforing, context.periode) ?: return@mapNotNull null
            createUtbetaling(context.gjennomforing.id, context.periode, beregning)
        }
    }

    fun beregnUtbetalingerForPeriode(periode: Periode): List<Utbetaling> {
        return getContextForBeregningAvUtbetalinger(periode).mapNotNull { context ->
            val beregning = beregnUtbetaling(context.gjennomforing, context.periode) ?: return@mapNotNull null
            UtbetalingMapper.toNewUtbetaling(context.periode, context.gjennomforing, beregning)
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
    fun oppdaterUtbetalingerForGjennomforing(gjennomforingId: UUID): List<Utbetaling> {
        val gjennomforing = getGjennomforing(gjennomforingId)
        return hentGenererteUtbetalinger(gjennomforingId).mapNotNull { utbetaling ->
            val oppdatertBeregning = beregnUtbetaling(gjennomforing, utbetaling.periode)

            if (oppdatertBeregning == null) {
                // TODO: sletting burde kanskje også gjøres via UtbetalingService
                log.info("Sletter utbetaling=${utbetaling.id} fordi den ikke lengre er relevant for arrangør")
                db.session { queries.utbetaling.delete(utbetaling.id) }
                return@mapNotNull null
            }

            if (oppdatertBeregning == utbetaling.beregning) {
                return@mapNotNull null
            }

            utbetalingService.oppdaterBeregning(utbetaling.id, oppdatertBeregning, Tiltaksadministrasjon).getOrElse {
                throw UtbetalingException(it)
            }
        }
    }

    fun oppdaterUtbetalingBlokkeringerForGjennomforing(gjennomforingId: UUID): List<Utbetaling> = db.transaction {
        val forslag = queries.deltakerForslag.getForslagByGjennomforing(gjennomforingId)

        return hentGenererteUtbetalinger(gjennomforingId).map { utbetaling ->
            val blokkeringer = blokkeringer(utbetaling.periode, utbetaling.beregning, forslag)
            queries.utbetaling.setBlokkeringer(utbetaling.id, blokkeringer)
            utbetaling.copy(blokkeringer = blokkeringer)
        }
    }

    suspend fun regenererUtbetaling(utbetaling: Utbetaling): Utbetaling {
        val utbetalingerSammePeriode = getUtbetalinger(utbetaling.gjennomforing.id)
            .filter { it.periode == utbetaling.periode }

        val alleredeRegenerert = utbetalingerSammePeriode
            .sortedByDescending { it.createdAt }
            .firstOrNull { it.status != UtbetalingStatusType.AVBRUTT }

        if (alleredeRegenerert != null) {
            throw IllegalArgumentException("Allerede regenerert med id=${alleredeRegenerert.id}")
        }

        val gjennomforing = getGjennomforing(utbetaling.gjennomforing.id)
        val beregning = requireNotNull(beregnUtbetaling(gjennomforing, utbetaling.periode)) {
            "Generert utbetaling med id=${utbetaling.id} kunne ikke beregnes på nytt fordi den ikke lengre er relevant for arrangør"
        }

        return createUtbetaling(utbetaling.gjennomforing.id, utbetaling.periode, beregning)
    }

    private fun hentGenererteUtbetalinger(gjennomforingId: UUID): List<Utbetaling> {
        return getUtbetalinger(gjennomforingId).filter {
            when (it.status) {
                UtbetalingStatusType.TIL_BEHANDLING,
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

    private fun getUtbetalinger(gjennomforingId: UUID): List<Utbetaling> = db.session {
        queries.utbetaling.getByGjennomforing(gjennomforingId)
    }

    private fun beregnUtbetaling(gjennomforing: GjennomforingAvtale, periode: Periode): UtbetalingBeregning? {
        if (!isValidUtbetalingPeriode(gjennomforing.tiltakstype.tiltakskode, periode)) {
            log.info("Genererer ikke utbetaling for gjennomforing=${gjennomforing.id} fordi utbetalingsperioden ikke er tillatt tiltakskode=${gjennomforing.tiltakstype.tiltakskode}, periode=$periode")
            return null
        }

        val type = gjennomforing.prismodell.type
        val prismodell = prismodeller.singleOrNull { it.type == type } ?: run {
            log.info("Genererer ikke utbetaling for gjennomføring=${gjennomforing.id} fordi prismodellen ikke er støttet type=$type")
            return null
        }

        val deltakere = db.session { queries.deltaker.getByGjennomforingId(gjennomforing.id) }
        return prismodell.beregn(gjennomforing, periode, deltakere).takeIf { it.output.pris.belop > 0 }
    }

    private suspend fun createUtbetaling(
        gjennomforingId: UUID,
        periode: Periode,
        beregning: UtbetalingBeregning,
    ): Utbetaling {
        val forrigeKrav = db.session { queries.utbetaling.getSisteGodkjenteUtbetaling(gjennomforingId) }
        val forrigeKid = when (forrigeKrav?.betalingsinformasjon) {
            is Betalingsinformasjon.BBan -> forrigeKrav.betalingsinformasjon.kid
            else -> null
        }
        val forslag = db.session { queries.deltakerForslag.getForslagByGjennomforing(gjennomforingId) }
        val blokkeringer = blokkeringer(periode, beregning, forslag)
        val opprett = UpsertUtbetaling.Generering(
            id = UUID.randomUUID(),
            gjennomforingId = gjennomforingId,
            periode = periode,
            beregning = beregning,
            tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
            kid = forrigeKid,
            blokkeringer = blokkeringer,
        )
        return utbetalingService.opprettUtbetaling(opprett, Tiltaksadministrasjon).getOrElse {
            throw UtbetalingException(it)
        }
    }

    private fun blokkeringer(
        periode: Periode,
        beregning: UtbetalingBeregning,
        forslag: Map<UUID, List<DeltakerForslag>>,
    ): Set<Utbetaling.Blokkering> {
        val relevanteForslag = UtbetalingAdvarsler.relevanteForslag(periode, beregning, forslag)

        return setOfNotNull(
            Utbetaling.Blokkering.UBEHANDLET_FORSLAG.takeIf {
                relevanteForslag.isNotEmpty()
            },
        )
    }

    private fun getContextForGenereringAvUtbetalinger(periode: Periode): List<UtbetalingContext> {
        return getContextForUtbetalinger(periode, includeNotExists = true)
    }

    private fun getContextForBeregningAvUtbetalinger(periode: Periode): List<UtbetalingContext> {
        return getContextForUtbetalinger(periode, includeNotExists = false)
    }

    private fun getContextForUtbetalinger(
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

    private fun getContextForPrismodell(
        prismodell: PrismodellType,
        tilskuddstype: Tilskuddstype,
        periode: Periode,
        includeNotExists: Boolean,
    ): List<UtbetalingContext> = db.session {
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
                and daterange(gjennomforing.start_dato, gjennomforing.slutt_dato, '[]') && :periode::daterange
                $notExistsClause
        """.trimIndent()

        val params = mapOf(
            "prismodell" to prismodell.name,
            "periode" to periode.toDaterange(),
            "tilskuddstype" to tilskuddstype.name,
        )

        return session.list(queryOf(query, params)) {
            val gjennomforing = getGjennomforing(it.uuid("id"))
            UtbetalingContext(gjennomforing, periode)
        }
    }

    private fun getGjennomforing(gjennomforingId: UUID): GjennomforingAvtale = db.session {
        queries.gjennomforing.getGjennomforingAvtaleOrError(gjennomforingId)
    }

    private fun isValidUtbetalingPeriode(tiltakskode: Tiltakskode, periode: Periode): Boolean {
        return config.gyldigTilsagnPeriode[tiltakskode]?.contains(periode) ?: false
    }
}
