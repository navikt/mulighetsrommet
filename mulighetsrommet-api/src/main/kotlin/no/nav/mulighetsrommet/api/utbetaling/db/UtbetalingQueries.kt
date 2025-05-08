package no.nav.mulighetsrommet.api.utbetaling.db

import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.tiltakstype.db.createArrayOfTiltakskode
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregning
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningForhandsgodkjent
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFri
import no.nav.mulighetsrommet.database.datatypes.periode
import no.nav.mulighetsrommet.database.datatypes.toDaterange
import no.nav.mulighetsrommet.database.requireSingle
import no.nav.mulighetsrommet.database.withTransaction
import no.nav.mulighetsrommet.model.*
import no.nav.tiltak.okonomi.Tilskuddstype
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
                prismodell,
                innsender,
                tilskuddstype,
                beskrivelse,
                godkjent_av_arrangor_tidspunkt
            ) values (
                :id::uuid,
                :gjennomforing_id::uuid,
                :frist_for_godkjenning,
                :kontonummer,
                :kid,
                :periode::daterange,
                :prismodell::prismodell,
                :innsender,
                :tilskuddstype::tilskuddstype,
                :beskrivelse,
                :godkjent_av_arrangor_tidspunkt
            ) on conflict (id) do update set
                gjennomforing_id = excluded.gjennomforing_id,
                frist_for_godkjenning = excluded.frist_for_godkjenning,
                kontonummer = excluded.kontonummer,
                kid = excluded.kid,
                periode = excluded.periode,
                prismodell = excluded.prismodell,
                innsender = excluded.innsender,
                tilskuddstype = excluded.tilskuddstype,
                beskrivelse = excluded.beskrivelse,
                godkjent_av_arrangor_tidspunkt = excluded.godkjent_av_arrangor_tidspunkt
        """.trimIndent()

        val params = mapOf(
            "id" to dbo.id,
            "gjennomforing_id" to dbo.gjennomforingId,
            "frist_for_godkjenning" to dbo.fristForGodkjenning,
            "kontonummer" to dbo.kontonummer?.value,
            "kid" to dbo.kid?.value,
            "periode" to dbo.periode.toDaterange(),
            "prismodell" to when (dbo.beregning) {
                is UtbetalingBeregningForhandsgodkjent -> Prismodell.FORHANDSGODKJENT.name
                is UtbetalingBeregningFri -> Prismodell.FRI.name
            },
            "innsender" to dbo.innsender?.textRepr(),
            "beskrivelse" to dbo.beskrivelse,
            "tilskuddstype" to dbo.tilskuddstype.name,
            "godkjent_av_arrangor_tidspunkt" to dbo.godkjentAvArrangorTidspunkt,
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
            insert into utbetaling_beregning_forhandsgodkjent (utbetaling_id, sats, belop)
            values (:utbetaling_id::uuid, :sats, :belop)
            on conflict (utbetaling_id) do update set
                sats = excluded.sats,
                belop = excluded.belop
        """.trimIndent()
        val params = mapOf(
            "utbetaling_id" to id,
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
            values (:utbetaling_id::uuid, :periode::daterange, :beskrivelse)
        """.trimIndent()
        val stengt = beregning.input.stengt.map { stengt ->
            mapOf(
                "utbetaling_id" to id,
                "periode" to stengt.periode.toDaterange(),
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
            values (:utbetaling_id, :deltakelse_id, :periode::daterange, :deltakelsesprosent)
        """.trimIndent()
        val perioder = beregning.input.deltakelser.flatMap { deltakelse ->
            deltakelse.perioder.map { periode ->
                mapOf(
                    "utbetaling_id" to id,
                    "deltakelse_id" to deltakelse.deltakelseId,
                    "periode" to periode.periode.toDaterange(),
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

    fun setGodkjentAvArrangor(id: UUID, tidspunkt: LocalDateTime) {
        @Language("PostgreSQL")
        val query = """
            update utbetaling set
                godkjent_av_arrangor_tidspunkt = :tidspunkt,
                innsender = 'Arrangor'
            where id = :id::uuid
        """.trimIndent()

        session.execute(queryOf(query, mapOf("id" to id, "tidspunkt" to tidspunkt)))
    }

    fun setKontonummer(id: UUID, kontonummer: Kontonummer) {
        @Language("PostgreSQL")
        val query = """
            update utbetaling
            set kontonummer = ?
            where id = ?::uuid
        """.trimIndent()

        session.execute(queryOf(query, kontonummer.value, id))
    }

    fun setKid(id: UUID, kid: Kid?) {
        @Language("PostgreSQL")
        val query = """
            update utbetaling
            set kid = ?
            where id = ?::uuid
        """.trimIndent()

        session.execute(queryOf(query, kid?.value, id))
    }

    fun setJournalpostId(id: UUID, journalpostId: String) {
        @Language("PostgreSQL")
        val query = """
            update utbetaling
            set journalpost_id = :journalpost_id
            where id = :id::uuid
        """.trimIndent()

        val params = mapOf("id" to id, "journalpost_id" to journalpostId)

        session.execute(queryOf(query, params))
    }

    fun get(id: UUID): Utbetaling? {
        @Language("PostgreSQL")
        val utbetalingQuery = """
            select *
            from utbetaling_dto_view
            where id = ?::uuid
        """.trimIndent()

        return session.single(queryOf(utbetalingQuery, id)) { it.toUtbetalingDto() }
    }

    fun getOppgaveData(tiltakskoder: Set<Tiltakskode>?): List<Utbetaling> {
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
            "tiltakskoder" to tiltakskoder?.let { session.createArrayOfTiltakskode(it) },
        )

        return session.list(queryOf(utbetalingQuery, params)) { it.toUtbetalingDto() }
    }

    fun getByArrangorIds(
        organisasjonsnummer: Organisasjonsnummer,
    ): List<Utbetaling> {
        @Language("PostgreSQL")
        val query = """
            select *
            from utbetaling_dto_view
            where arrangor_organisasjonsnummer = ?
            order by frist_for_godkjenning desc
        """.trimIndent()

        return session.list(queryOf(query, organisasjonsnummer.value)) { it.toUtbetalingDto() }
    }

    fun getByGjennomforing(gjennomforingId: UUID): List<Utbetaling> = with(session) {
        @Language("PostgreSQL")
        val query = """
            select *
            from utbetaling_dto_view
            where gjennomforing_id = :id::uuid
        """.trimIndent()

        val params = mapOf("id" to gjennomforingId)

        return list(queryOf(query, params)) { it.toUtbetalingDto() }
    }

    fun getSisteGodkjenteUtbetaling(gjennomforingId: UUID): Utbetaling? {
        @Language("PostgreSQL")
        val query = """
            select *
            from utbetaling_dto_view
            where gjennomforing_id = ?::uuid
            order by godkjent_av_arrangor_tidspunkt desc
            limit 1
        """.trimIndent()

        return session.single(queryOf(query, gjennomforingId)) {
            it.toUtbetalingDto()
        }
    }

    private fun Row.toUtbetalingDto(): Utbetaling {
        val id = uuid("id")
        val beregning = getBeregning(id, Prismodell.valueOf(string("prismodell")))
        val innsender = stringOrNull("innsender")?.toAgent()
        return Utbetaling(
            id = id,
            fristForGodkjenning = localDateTime("frist_for_godkjenning"),
            godkjentAvArrangorTidspunkt = localDateTimeOrNull("godkjent_av_arrangor_tidspunkt"),
            gjennomforing = Utbetaling.Gjennomforing(
                id = uuid("gjennomforing_id"),
                navn = string("gjennomforing_navn"),
            ),
            arrangor = Utbetaling.Arrangor(
                id = uuid("arrangor_id"),
                organisasjonsnummer = Organisasjonsnummer(string("arrangor_organisasjonsnummer")),
                navn = string("arrangor_navn"),
                slettet = boolean("arrangor_slettet"),
            ),
            tiltakstype = Utbetaling.Tiltakstype(
                navn = string("tiltakstype_navn"),
                tiltakskode = Tiltakskode.valueOf(string("tiltakskode")),
            ),
            beregning = beregning,
            betalingsinformasjon = Utbetaling.Betalingsinformasjon(
                kontonummer = stringOrNull("kontonummer")?.let { Kontonummer(it) },
                kid = stringOrNull("kid")?.let { Kid(it) },
            ),
            journalpostId = stringOrNull("journalpost_id"),
            periode = periode("periode"),
            innsender = innsender,
            createdAt = localDateTime("created_at"),
            beskrivelse = stringOrNull("beskrivelse"),
            tilskuddstype = Tilskuddstype.valueOf(string("tilskuddstype")),
        )
    }

    private fun getBeregning(id: UUID, prismodell: Prismodell): UtbetalingBeregning {
        return when (prismodell) {
            Prismodell.FORHANDSGODKJENT -> getBeregningForhandsgodkjent(id)
            Prismodell.FRI -> getBeregningFri(id)
        }
    }

    private fun getBeregningForhandsgodkjent(id: UUID): UtbetalingBeregning {
        @Language("PostgreSQL")
        val query = """
            select *
            from view_utbetaling_beregning_forhandsgodkjent
            where utbetaling_id = ?::uuid
        """.trimIndent()

        return session.requireSingle(queryOf(query, id)) { row ->
            UtbetalingBeregningForhandsgodkjent(
                input = UtbetalingBeregningForhandsgodkjent.Input(
                    periode = row.periode("periode"),
                    sats = row.int("sats"),
                    stengt = Json.decodeFromString(row.string("stengt_json")),
                    deltakelser = Json.decodeFromString(row.string("perioder_json")),
                ),
                output = UtbetalingBeregningForhandsgodkjent.Output(
                    belop = row.int("belop"),
                    deltakelser = Json.decodeFromString(row.string("manedsverk_json")),
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
