package no.nav.mulighetsrommet.api.refusjon.db

import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.refusjon.model.*
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

class RefusjonskravQueries(private val session: Session) {

    fun upsert(dbo: RefusjonskravDbo) = withTransaction(session) {
        @Language("PostgreSQL")
        val refusjonskravQuery = """
            insert into refusjonskrav (id, gjennomforing_id, frist_for_godkjenning, kontonummer, kid, periode, beregningsmodell)
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
            "beregningsmodell" to when(dbo.beregning) {
                is RefusjonKravBeregningAft -> Beregningsmodell.FORHANDSGODKJENT
                is RefusjonKravBeregningFri -> Beregningsmodell.FRI
            }
        )

        execute(queryOf(refusjonskravQuery, params))

        when (dbo.beregning) {
            is RefusjonKravBeregningAft -> {
                upsertRefusjonskravBeregningAft(dbo.id, dbo.beregning)
            }

            is RefusjonKravBeregningFri -> {
                upsertRefusjonskravBeregningFri(dbo.id, dbo.beregning)
            }
        }
    }

    private fun upsertRefusjonskravBeregningAft(
        id: UUID,
        beregning: RefusjonKravBeregningAft,
    ) = withTransaction(session) {
        @Language("PostgreSQL")
        val query = """
            insert into refusjonskrav_beregning_aft (refusjonskrav_id, periode, sats, belop)
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
            from refusjonskrav_deltakelse_periode
            where refusjonskrav_id = ?::uuid;
        """
        execute(queryOf(deletePerioderQuery, id))

        @Language("PostgreSQL")
        val insertPeriodeQuery = """
            insert into refusjonskrav_deltakelse_periode (refusjonskrav_id, deltakelse_id, periode, deltakelsesprosent)
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
            from refusjonskrav_deltakelse_manedsverk
            where refusjonskrav_id = ?::uuid
        """
        execute(queryOf(deleteManedsverk, id))

        @Language("PostgreSQL")
        val insertManedsverkQuery = """
            insert into refusjonskrav_deltakelse_manedsverk (refusjonskrav_id, deltakelse_id, manedsverk)
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

    fun upsertRefusjonskravBeregningFri(id: UUID, beregning: RefusjonKravBeregningFri) {
        @Language("PostgreSQL")
        val query = """
            insert into refusjonskrav_beregning_fri (refusjonskrav_id, belop)
            values (:refusjonskrav_id::uuid, :belop)
            on conflict (refusjonskrav_id) do update set
                belop = excluded.belop
        """.trimIndent()

        val params = mapOf(
            "refusjonskrav_id" to id,
            "belop" to beregning.output.belop
        )

        session.execute(queryOf(query, params))
    }

    fun setGodkjentAvArrangor(id: UUID, tidspunkt: LocalDateTime) = with(session) {
        @Language("PostgreSQL")
        val query = """
            update refusjonskrav
            set godkjent_av_arrangor_tidspunkt = :tidspunkt
            where id = :id::uuid
        """.trimIndent()

        execute(queryOf(query, mapOf("id" to id, "tidspunkt" to tidspunkt)))
    }

    fun setBetalingsInformasjon(id: UUID, kontonummer: Kontonummer, kid: Kid?) = with(session) {
        @Language("PostgreSQL")
        val query = """
            update refusjonskrav
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
            update refusjonskrav
            set journalpost_id = :journalpost_id
            where id = :id::uuid
        """.trimIndent()

        val params = mapOf("id" to id, "journalpost_id" to journalpostId)

        execute(queryOf(query, params))
    }

    fun get(id: UUID): RefusjonskravDto? = with(session) {
        @Language("PostgreSQL")
        val refusjonskravQuery = """
            select *
            from refusjonskrav_admin_dto_view
            where id = ?::uuid
        """.trimIndent()

        return single(queryOf(refusjonskravQuery, id)) { it.toRefusjonskrav() }
    }

    fun getByArrangorIds(
        organisasjonsnummer: Organisasjonsnummer,
    ): List<RefusjonskravDto> = with(session) {
        @Language("PostgreSQL")
        val query = """
            select *
            from refusjonskrav_admin_dto_view
            where arrangor_organisasjonsnummer = ?
            order by frist_for_godkjenning desc
        """.trimIndent()

        return list(queryOf(query, organisasjonsnummer.value)) { it.toRefusjonskrav() }
    }

    fun getByGjennomforing(gjennomforingId: UUID, statuser: List<RefusjonskravStatus>? = null): List<RefusjonskravDto> =
        with(session) {
            @Language("PostgreSQL")
            val query = """
            select *
            from refusjonskrav_admin_dto_view
            where
                gjennomforing_id = :id::uuid
                and (:statuser::refusjonskrav_status[] is null or status = any(:statuser))
        """.trimIndent()

            val params = mapOf(
                "id" to gjennomforingId,
                "statuser" to statuser?.ifEmpty { null }?.let { createEnumArray("refusjonskrav_status", it) },
            )

            return list(queryOf(query, params)) { it.toRefusjonskrav() }
        }

    fun getBeregning(id: UUID, beregningsmodell: Beregningsmodell): RefusjonKravBeregning {
        return when (beregningsmodell) {
            Beregningsmodell.FORHANDSGODKJENT -> getBeregningAft(id)
            Beregningsmodell.FRI -> getBeregningFri(id)
        }
    }

    private fun getBeregningAft(id: UUID): RefusjonKravBeregning {
        @Language("PostgreSQL")
        val query = """
            select *
            from refusjonskrav_aft_view
            where
                id = :id::uuid
        """.trimIndent()

        return session.requireSingle(queryOf(query, mapOf("id" to id))) { it.toRefusjonsKravAft().beregning }
    }

    private fun getBeregningFri(id: UUID): RefusjonKravBeregning {
        @Language("PostgreSQL")
        val query = """
            select *
            from refusjonskrav_beregning_fri
            where
                refusjonskrav_id = :id::uuid
        """.trimIndent()

        return session.requireSingle(queryOf(query, mapOf("id" to id))) {
            RefusjonKravBeregningFri(
                input = RefusjonKravBeregningFri.Input(
                    belop = it.int("belop")
                ),
                output = RefusjonKravBeregningFri.Output(
                    belop = it.int("belop")
                )
            )
        }
    }


    fun getSisteGodkjenteRefusjonskrav(gjennomforingId: UUID): RefusjonskravDto? = with(session) {
        @Language("PostgreSQL")
        val query = """
            select *
            from refusjonskrav_aft_view
            where gjennomforing_id = ?::uuid
            order by godkjent_av_arrangor_tidspunkt desc
            limit 1
        """.trimIndent()

        return single(queryOf(query, gjennomforingId)) {
            it.toRefusjonskrav()
        }
    }

    fun Row.toRefusjonskrav(): RefusjonskravDto {
        val beregningsmodell = Beregningsmodell.valueOf(string("beregningsmodell"))
        val beregning = getBeregning(uuid("id"), beregningsmodell)

        return toRefusjonskravDto(beregning)
    }
}

private fun Row.toRefusjonsKravAft(): RefusjonskravDto {
    val beregning = RefusjonKravBeregningAft(
        input = RefusjonKravBeregningAft.Input(
            periode = Periode(localDate("beregning_periode_start"), localDate("beregning_periode_slutt")),
            sats = int("sats"),
            deltakelser = stringOrNull("perioder_json")?.let { Json.decodeFromString(it) } ?: setOf(),
        ),
        output = RefusjonKravBeregningAft.Output(
            belop = int("belop"),
            deltakelser = stringOrNull("manedsverk_json")?.let { Json.decodeFromString(it) } ?: setOf(),
        ),
    )
    return toRefusjonskravDto(beregning)
}

private fun Row.toRefusjonskravDto(beregning: RefusjonKravBeregning): RefusjonskravDto {
    return RefusjonskravDto(
        id = uuid("id"),
        beregningsmodell = Beregningsmodell.valueOf(string("beregningsmodell")),
        status = RefusjonskravStatus.valueOf(string("status")),
        fristForGodkjenning = localDateTime("frist_for_godkjenning"),
        gjennomforing = RefusjonskravDto.Gjennomforing(
            id = uuid("gjennomforing_id"),
            navn = string("gjennomforing_navn"),
        ),
        arrangor = RefusjonskravDto.Arrangor(
            id = uuid("arrangor_id"),
            organisasjonsnummer = Organisasjonsnummer(string("arrangor_organisasjonsnummer")),
            navn = string("arrangor_navn"),
            slettet = boolean("arrangor_slettet"),
        ),
        tiltakstype = RefusjonskravDto.Tiltakstype(
            navn = string("tiltakstype_navn"),
        ),
        beregning = beregning,
        betalingsinformasjon = RefusjonskravDto.Betalingsinformasjon(
            kontonummer = stringOrNull("kontonummer")?.let { Kontonummer(it) },
            kid = stringOrNull("kid")?.let { Kid(it) },
        ),
        journalpostId = stringOrNull("journalpost_id"),
        periodeStart = localDate("periode_start"),
        periodeSlutt = localDate("periode_slutt"),
    )
}
