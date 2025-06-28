package no.nav.mulighetsrommet.api.utbetaling

import arrow.core.Either
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.clients.kontoregisterOrganisasjon.KontoregisterOrganisasjonClient
import no.nav.mulighetsrommet.api.endringshistorikk.DocumentClass
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingDto
import no.nav.mulighetsrommet.api.tilsagn.model.ForhandsgodkjenteSatser
import no.nav.mulighetsrommet.api.utbetaling.db.UtbetalingDbo
import no.nav.mulighetsrommet.api.utbetaling.model.*
import no.nav.mulighetsrommet.model.*
import no.nav.tiltak.okonomi.Tilskuddstype
import org.intellij.lang.annotations.Language
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class GenererUtbetalingService(
    private val db: ApiDatabase,
    private val kontoregisterOrganisasjonClient: KontoregisterOrganisasjonClient,
) {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    suspend fun genererUtbetalingForMonth(month: Int): List<Utbetaling> = db.transaction {
        val currentYear = LocalDate.now().year
        val date = LocalDate.of(currentYear, month, 1)
        val periode = Periode.forMonthOf(date)

        getGjennomforingerForGenereringAvUtbetalinger(periode)
            .mapNotNull { (gjennomforingId, avtaletype) ->
                val utbetaling = when (avtaletype) {
                    Avtaletype.FORHANDSGODKJENT -> createUtbetalingForhandsgodkjent(
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
            gjennomforingId = gjennomforingId,
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

    private fun QueryContext.getOrError(id: UUID): Utbetaling {
        return queries.utbetaling.getOrError(id)
    }
}

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
