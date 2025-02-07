package no.nav.mulighetsrommet.api.utbetaling.db

import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.utbetaling.model.*
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningAft
import no.nav.mulighetsrommet.database.createEnumArray
import no.nav.mulighetsrommet.database.requireSingle
import no.nav.mulighetsrommet.database.withTransaction
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Periode
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime
import java.util.*

class UtbetalingQueries(private val session: Session) {
    fun upsert(dbo: UtbetalingDbo) = withTransaction(session) {
        @Language("PostgreSQL")
        val utbetalingQuery = """
            insert into utbetaling (id, gjennomforing_id, frist_for_godkjenning, kontonummer, kid, periode, beregningsmodell)
            values (:id::uuid, :gjennomforing_id::uuid, :frist_for_godkjenning, :kontonummer, :kid, daterange(:periode_start, :periode_slutt), :beregningsmodell::beregningsmodell)
            on conflict (id) do update set
                gjennomforing_id = excluded.gjennomforing_id,
                frist_for_godkjenning = excluded.frist_for_godkjenning,
                kontonummer = excluded.kontonummer,
                kid = excluded.kid,
                periode = excluded.periode,
                beregningsmodell = excluded.beregningsmodell
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
                is UtbetalingBeregningAft -> Beregningsmodell.FORHANDSGODKJENT.name
                is UtbetalingBeregningFri -> Beregningsmodell.FRI.name
            },
        )

        execute(queryOf(utbetalingQuery, params))

        when (dbo.beregning) {
            is UtbetalingBeregningAft -> {
                upsertUtbetalingBeregningAft(dbo.id, dbo.beregning)
            }

            is UtbetalingBeregningFri -> {
                upsertUtbetalingBeregningFri(dbo.id, dbo.beregning)
            }
        }
    }

    private fun upsertUtbetalingBeregningAft(
        id: UUID,
        beregning: UtbetalingBeregningAft,
    ) = withTransaction(session) {
        @Language("PostgreSQL")
        val query = """
            insert into utbetaling_beregning_aft (refusjonskrav_id, periode, sats, belop)
            values (:refusjonskrav_id::uuid, daterange(:periode_start, :periode_slutt), :sats, :belop)
            on conflict (refusjonskrav_id) do update set
                periode = excluded.periode,
                sats = excluded.sats,
                belop = excluded.belop
        """.trimIndent()

        val params = mapOf(
            "refusjonskrav_id" to id,
            "periode_start" to beregning.input.periode.start,
            "periode_slutt" to beregning.input.periode.slutt,
            "sats" to beregning.input.sats,
            "belop" to beregning.output.belop,
        )
        execute(queryOf(query, params))

        @Language("PostgreSQL")
        val deletePerioderQuery = """
            delete
            from utbetaling_deltakelse_periode
            where refusjonskrav_id = ?::uuid;
        """
        execute(queryOf(deletePerioderQuery, id))

        @Language("PostgreSQL")
        val insertPeriodeQuery = """
            insert into utbetaling_deltakelse_periode (refusjonskrav_id, deltakelse_id, periode, deltakelsesprosent)
            values (:refusjonskrav_id, :deltakelse_id, daterange(:start, :slutt), :deltakelsesprosent)
        """.trimIndent()

        val perioder = beregning.input.deltakelser.flatMap { deltakelse ->
            deltakelse.perioder.map { periode ->
                mapOf(
                    "refusjonskrav_id" to id,
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
            where refusjonskrav_id = ?::uuid
        """
        execute(queryOf(deleteManedsverk, id))

        @Language("PostgreSQL")
        val insertManedsverkQuery = """
            insert into utbetaling_deltakelse_manedsverk (refusjonskrav_id, deltakelse_id, manedsverk)
            values (:refusjonskrav_id, :deltakelse_id, :manedsverk)
        """.trimIndent()

        val manedsverk = beregning.output.deltakelser.map { deltakelse ->
            mapOf(
                "refusjonskrav_id" to id,
                "deltakelse_id" to deltakelse.deltakelseId,
                "manedsverk" to deltakelse.manedsverk,
            )
        }
        batchPreparedNamedStatement(insertManedsverkQuery, manedsverk)
    }

    fun upsertUtbetalingBeregningFri(id: UUID, beregning: UtbetalingBeregningFri) {
        @Language("PostgreSQL")
        val query = """
            insert into refusjonskrav_beregning_fri (refusjonskrav_id, belop)
            values (:refusjonskrav_id::uuid, :belop)
            on conflict (refusjonskrav_id) do update set
                belop = excluded.belop
        """.trimIndent()

        val params = mapOf(
            "refusjonskrav_id" to id,
            "belop" to beregning.output.belop,
        )

        session.execute(queryOf(query, params))
    }

    fun setGodkjentAvArrangor(id: UUID, tidspunkt: LocalDateTime) = with(session) {
        @Language("PostgreSQL")
        val query = """
            update utbetaling
            set godkjent_av_arrangor_tidspunkt = :tidspunkt
            where id = :id::uuid
        """.trimIndent()

        execute(queryOf(query, mapOf("id" to id, "tidspunkt" to tidspunkt)))
    }

    fun setBetalingsInformasjon(id: UUID, kontonummer: Kontonummer, kid: Kid?) = with(session) {
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

    fun getByGjennomforing(gjennomforingId: UUID, statuser: List<UtbetalingStatus>? = null): List<UtbetalingDto> = with(session) {
        @Language("PostgreSQL")
        val query = """
            select *
            from utbetaling_dto_view
            where
                gjennomforing_id = :id::uuid
                and (:statuser::utbetaling_status[] is null or status = any(:statuser))
        """.trimIndent()

        val params = mapOf(
            "id" to gjennomforingId,
            "statuser" to statuser?.ifEmpty { null }?.let { createEnumArray("utbetaling_status", it) },
        )

        return list(queryOf(query, params)) { it.toUtbetalingDto() }
    }

    fun getBeregning(id: UUID, beregningsmodell: Beregningsmodell): UtbetalingBeregning {
        return when (beregningsmodell) {
            Beregningsmodell.FORHANDSGODKJENT -> getBeregningAft(id)
            Beregningsmodell.FRI -> getBeregningFri(id)
        }
    }

    private fun getBeregningAft(id: UUID): UtbetalingBeregning {
        @Language("PostgreSQL")
        val query = """
            select *
            from utbetaling_aft_view
            where
                refusjonskrav_id = :id::uuid
        """.trimIndent()

        return session.requireSingle(queryOf(query, mapOf("id" to id))) {
            UtbetalingBeregningAft(
                input = UtbetalingBeregningAft.Input(
                    periode = Periode(it.localDate("beregning_periode_start"), it.localDate("beregning_periode_slutt")),
                    sats = it.int("sats"),
                    deltakelser = it.stringOrNull("perioder_json")?.let { Json.decodeFromString(it) } ?: setOf(),
                ),
                output = UtbetalingBeregningAft.Output(
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
            where
                refusjonskrav_id = :id::uuid
        """.trimIndent()

        return session.requireSingle(queryOf(query, mapOf("id" to id))) {
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

    fun getSisteGodkjenteUtbetaling(gjennomforingId: UUID): UtbetalingDto? = with(session) {
        @Language("PostgreSQL")
        val query = """
            select *
            from utbetaling_aft_view
            where gjennomforing_id = ?::uuid
            order by godkjent_av_arrangor_tidspunkt desc
            limit 1
        """.trimIndent()

        return single(queryOf(query, gjennomforingId)) {
            it.toUtbetalingDto()
        }
    }

    fun Row.toUtbetalingDto(): UtbetalingDto {
        val beregningsmodell = Beregningsmodell.valueOf(string("beregningsmodell"))
        val beregning = getBeregning(uuid("id"), beregningsmodell)

        return UtbetalingDto(
            id = uuid("id"),
            status = UtbetalingStatus.valueOf(string("status")),
            fristForGodkjenning = localDateTime("frist_for_godkjenning"),
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
        )
    }
}
