package no.nav.mulighetsrommet.api.utbetaling.db

import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.utbetaling.model.*
import no.nav.mulighetsrommet.database.requireSingle
import no.nav.mulighetsrommet.database.withTransaction
import no.nav.mulighetsrommet.model.*
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime
import java.util.*

class UtbetalingQueries(private val session: Session) {
    fun upsert(dbo: UtbetalingDbo) = withTransaction(session) {
        @Language("PostgreSQL")
        val utbetalingQuery = """
            insert into utbetaling (
                id,
                gjennomforing_id,
                frist_for_godkjenning,
                kontonummer,
                kid,
                periode,
                beregningsmodell,
                innsender
            ) values (
                :id::uuid,
                :gjennomforing_id::uuid,
                :frist_for_godkjenning,
                :kontonummer,
                :kid,
                daterange(:periode_start, :periode_slutt),
                :beregningsmodell::beregningsmodell,
                :innsender
            ) on conflict (id) do update set
                gjennomforing_id = excluded.gjennomforing_id,
                frist_for_godkjenning = excluded.frist_for_godkjenning,
                kontonummer = excluded.kontonummer,
                kid = excluded.kid,
                periode = excluded.periode,
                beregningsmodell = excluded.beregningsmodell,
                innsender = excluded.innsender
        """.trimIndent()

        val params = mapOf(
            "id" to dbo.id,
            "gjennomforing_id" to dbo.gjennomforingId,
            "frist_for_godkjenning" to dbo.fristForGodkjenning,
            "kontonummer" to dbo.kontonummer?.value,
            "kid" to dbo.kid?.value,
            "periode_start" to dbo.periode.start,
            "periode_slutt" to dbo.periode.slutt,
            "beregningsmodell" to when (dbo.beregning) {
                is UtbetalingBeregningForhandsgodkjent -> Beregningsmodell.FORHANDSGODKJENT.name
                is UtbetalingBeregningFri -> Beregningsmodell.FRI.name
            },
            "innsender" to dbo.innsender?.value,
        )

        execute(queryOf(utbetalingQuery, params))

        when (dbo.beregning) {
            is UtbetalingBeregningForhandsgodkjent -> {
                check(dbo.periode == dbo.beregning.input.periode) {
                    "Utbetalingsperiode og beregningsperiode må være lik"
                }
                upsertUtbetalingBeregningAft(dbo.id, dbo.beregning)
            }

            is UtbetalingBeregningFri -> {
                upsertUtbetalingBeregningFri(dbo.id, dbo.beregning)
            }
        }
    }

    private fun Session.upsertUtbetalingBeregningAft(
        id: UUID,
        beregning: UtbetalingBeregningForhandsgodkjent,
    ) {
        @Language("PostgreSQL")
        val query = """
            insert into utbetaling_beregning_forhandsgodkjent (utbetaling_id, periode, sats, belop)
            values (:utbetaling_id::uuid, daterange(:periode_start, :periode_slutt), :sats, :belop)
            on conflict (utbetaling_id) do update set
                periode = excluded.periode,
                sats = excluded.sats,
                belop = excluded.belop
        """.trimIndent()
        val params = mapOf(
            "utbetaling_id" to id,
            "periode_start" to beregning.input.periode.start,
            "periode_slutt" to beregning.input.periode.slutt,
            "sats" to beregning.input.sats,
            "belop" to beregning.output.belop,
        )
        execute(queryOf(query, params))

        @Language("PostgreSQL")
        val deleteStengtHosArrangorQuery = """
            delete
            from utbetaling_stengt_hos_arrangor
            where utbetaling_id = ?::uuid
        """.trimIndent()
        execute(queryOf(deleteStengtHosArrangorQuery, id))

        @Language("PostgreSQL")
        val insertStengtHosArrangorQuery = """
            insert into utbetaling_stengt_hos_arrangor (utbetaling_id, periode, beskrivelse)
            values (:utbetaling_id::uuid, daterange(:start, :slutt), :beskrivelse)
        """.trimIndent()
        val stengt = beregning.input.stengt.map { stengt ->
            mapOf(
                "utbetaling_id" to id,
                "start" to stengt.start,
                "slutt" to stengt.slutt,
                "beskrivelse" to stengt.beskrivelse,
            )
        }
        batchPreparedNamedStatement(insertStengtHosArrangorQuery, stengt)

        @Language("PostgreSQL")
        val deletePerioderQuery = """
            delete
            from utbetaling_deltakelse_periode
            where utbetaling_id = ?::uuid;
        """
        execute(queryOf(deletePerioderQuery, id))

        @Language("PostgreSQL")
        val insertPeriodeQuery = """
            insert into utbetaling_deltakelse_periode (utbetaling_id, deltakelse_id, periode, deltakelsesprosent)
            values (:utbetaling_id, :deltakelse_id, daterange(:start, :slutt), :deltakelsesprosent)
        """.trimIndent()
        val perioder = beregning.input.deltakelser.flatMap { deltakelse ->
            deltakelse.perioder.map { periode ->
                mapOf(
                    "utbetaling_id" to id,
                    "deltakelse_id" to deltakelse.deltakelseId,
                    "start" to periode.start,
                    "slutt" to periode.slutt,
                    "deltakelsesprosent" to periode.deltakelsesprosent,
                )
            }
        }
        batchPreparedNamedStatement(insertPeriodeQuery, perioder)

        @Language("PostgreSQL")
        val deleteManedsverk = """
            delete
            from utbetaling_deltakelse_manedsverk
            where utbetaling_id = ?::uuid
        """
        execute(queryOf(deleteManedsverk, id))

        @Language("PostgreSQL")
        val insertManedsverkQuery = """
            insert into utbetaling_deltakelse_manedsverk (utbetaling_id, deltakelse_id, manedsverk)
            values (:utbetaling_id, :deltakelse_id, :manedsverk)
        """.trimIndent()

        val manedsverk = beregning.output.deltakelser.map { deltakelse ->
            mapOf(
                "utbetaling_id" to id,
                "deltakelse_id" to deltakelse.deltakelseId,
                "manedsverk" to deltakelse.manedsverk,
            )
        }
        batchPreparedNamedStatement(insertManedsverkQuery, manedsverk)
    }

    private fun Session.upsertUtbetalingBeregningFri(id: UUID, beregning: UtbetalingBeregningFri) {
        @Language("PostgreSQL")
        val query = """
            insert into utbetaling_beregning_fri (utbetaling_id, belop)
            values (:utbetaling_id::uuid, :belop)
            on conflict (utbetaling_id) do update set
                belop = excluded.belop
        """.trimIndent()

        val params = mapOf(
            "utbetaling_id" to id,
            "belop" to beregning.output.belop,
        )

        execute(queryOf(query, params))
    }

    fun setGodkjentAvArrangor(id: UUID, tidspunkt: LocalDateTime) = with(session) {
        @Language("PostgreSQL")
        val query = """
            update utbetaling set
                godkjent_av_arrangor_tidspunkt = :tidspunkt,
                innsender = 'ARRANGOR_ANSATT'
            where id = :id::uuid
        """.trimIndent()

        execute(queryOf(query, mapOf("id" to id, "tidspunkt" to tidspunkt)))
    }

    fun setBetalingsinformasjon(id: UUID, kontonummer: Kontonummer, kid: Kid?) = with(session) {
        @Language("PostgreSQL")
        val query = """
            update utbetaling
            set kontonummer = :kontonummer, kid = :kid
            where id = :id::uuid
        """.trimIndent()

        val params = mapOf(
            "id" to id,
            "kontonummer" to kontonummer.value,
            "kid" to kid?.value,
        )

        execute(queryOf(query, params))
    }

    fun setJournalpostId(id: UUID, journalpostId: String) = with(session) {
        @Language("PostgreSQL")
        val query = """
            update utbetaling
            set journalpost_id = :journalpost_id
            where id = :id::uuid
        """.trimIndent()

        val params = mapOf("id" to id, "journalpost_id" to journalpostId)

        execute(queryOf(query, params))
    }

    fun get(id: UUID): UtbetalingDto? = with(session) {
        @Language("PostgreSQL")
        val utbetalingQuery = """
            select *
            from utbetaling_dto_view
            where id = ?::uuid
        """.trimIndent()

        return single(queryOf(utbetalingQuery, id)) { it.toUtbetalingDto() }
    }

    fun getOppgaveData(tiltakskoder: List<Tiltakskode>?): List<UtbetalingDto> = with(session) {
        @Language("PostgreSQL")
        val utbetalingQuery = """
            select *
            from utbetaling_dto_view
                left join delutbetaling on delutbetaling.utbetaling_id = utbetaling_dto_view.id
            where
                delutbetaling.tilsagn_id is null and
                (:tiltakskoder::tiltakskode[] is null or tiltakskode = any(:tiltakskoder::tiltakskode[]))
        """.trimIndent()

        val params = mapOf(
            "tiltakskoder" to tiltakskoder?.let { session.createArrayOf("tiltakskode", it) },
        )

        return list(queryOf(utbetalingQuery, params)) { it.toUtbetalingDto() }
    }

    fun getByArrangorIds(
        organisasjonsnummer: Organisasjonsnummer,
    ): List<UtbetalingDto> = with(session) {
        @Language("PostgreSQL")
        val query = """
            select *
            from utbetaling_dto_view
            where arrangor_organisasjonsnummer = ?
            order by frist_for_godkjenning desc
        """.trimIndent()

        return list(queryOf(query, organisasjonsnummer.value)) { it.toUtbetalingDto() }
    }

    fun getByGjennomforing(gjennomforingId: UUID): List<UtbetalingDto> = with(session) {
        @Language("PostgreSQL")
        val query = """
            select *
            from utbetaling_dto_view
            where gjennomforing_id = :id::uuid
        """.trimIndent()

        val params = mapOf("id" to gjennomforingId)

        return list(queryOf(query, params)) { it.toUtbetalingDto() }
    }

    fun getSisteGodkjenteUtbetaling(gjennomforingId: UUID): UtbetalingDto? = with(session) {
        @Language("PostgreSQL")
        val query = """
            select *
            from utbetaling_dto_view
            where gjennomforing_id = ?::uuid
            order by godkjent_av_arrangor_tidspunkt desc
            limit 1
        """.trimIndent()

        return single(queryOf(query, gjennomforingId)) {
            it.toUtbetalingDto()
        }
    }

    private fun Row.toUtbetalingDto(): UtbetalingDto {
        val beregningsmodell = Beregningsmodell.valueOf(string("beregningsmodell"))
        val beregning = getBeregning(uuid("id"), beregningsmodell)
        val id = uuid("id")
        val innsender = stringOrNull("innsender")?.let { UtbetalingDto.Innsender.fromString(it) }

        return UtbetalingDto(
            id = id,
            fristForGodkjenning = localDateTime("frist_for_godkjenning"),
            godkjentAvArrangorTidspunkt = localDateTimeOrNull("godkjent_av_arrangor_tidspunkt"),
            gjennomforing = UtbetalingDto.Gjennomforing(
                id = uuid("gjennomforing_id"),
                navn = string("gjennomforing_navn"),
            ),
            arrangor = UtbetalingDto.Arrangor(
                id = uuid("arrangor_id"),
                organisasjonsnummer = Organisasjonsnummer(string("arrangor_organisasjonsnummer")),
                navn = string("arrangor_navn"),
                slettet = boolean("arrangor_slettet"),
            ),
            tiltakstype = UtbetalingDto.Tiltakstype(
                navn = string("tiltakstype_navn"),
                tiltakskode = Tiltakskode.valueOf(string("tiltakskode")),
            ),
            beregning = beregning,
            betalingsinformasjon = UtbetalingDto.Betalingsinformasjon(
                kontonummer = stringOrNull("kontonummer")?.let { Kontonummer(it) },
                kid = stringOrNull("kid")?.let { Kid(it) },
            ),
            journalpostId = stringOrNull("journalpost_id"),
            periode = Periode(
                start = localDate("periode_start"),
                slutt = localDate("periode_slutt"),
            ),
            innsender = innsender,
            createdAt = localDateTime("created_at"),
        )
    }

    private fun getBeregning(id: UUID, beregningsmodell: Beregningsmodell): UtbetalingBeregning {
        return when (beregningsmodell) {
            Beregningsmodell.FORHANDSGODKJENT -> getBeregningForhandsgodkjent(id)
            Beregningsmodell.FRI -> getBeregningFri(id)
        }
    }

    private fun getBeregningForhandsgodkjent(id: UUID): UtbetalingBeregning {
        @Language("PostgreSQL")
        val query = """
            select *
            from view_utbetaling_beregning_forhandsgodkjent
            where utbetaling_id = ?::uuid
        """.trimIndent()

        return session.requireSingle(queryOf(query, id)) {
            UtbetalingBeregningForhandsgodkjent(
                input = UtbetalingBeregningForhandsgodkjent.Input(
                    periode = Periode(it.localDate("beregning_periode_start"), it.localDate("beregning_periode_slutt")),
                    sats = it.int("sats"),
                    stengt = it.string("stengt_json").let { Json.decodeFromString(it) },
                    deltakelser = it.stringOrNull("perioder_json")?.let { Json.decodeFromString(it) } ?: setOf(),
                ),
                output = UtbetalingBeregningForhandsgodkjent.Output(
                    belop = it.int("belop"),
                    deltakelser = it.stringOrNull("manedsverk_json")?.let { Json.decodeFromString(it) } ?: setOf(),
                ),
            )
        }
    }

    private fun getBeregningFri(id: UUID): UtbetalingBeregning {
        @Language("PostgreSQL")
        val query = """
            select *
            from utbetaling_beregning_fri
            where utbetaling_id = ?::uuid
        """.trimIndent()

        return session.requireSingle(queryOf(query, id)) {
            UtbetalingBeregningFri(
                input = UtbetalingBeregningFri.Input(
                    belop = it.int("belop"),
                ),
                output = UtbetalingBeregningFri.Output(
                    belop = it.int("belop"),
                ),
            )
        }
    }
}
