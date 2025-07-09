package no.nav.mulighetsrommet.api.utbetaling.db

import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.tiltakstype.db.createArrayOfTiltakskode
import no.nav.mulighetsrommet.api.utbetaling.model.*
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
                kontonummer,
                kid,
                periode,
                beregning_type,
                belop_beregnet,
                innsender,
                tilskuddstype,
                beskrivelse,
                godkjent_av_arrangor_tidspunkt
            ) values (
                :id::uuid,
                :gjennomforing_id::uuid,
                :kontonummer,
                :kid,
                :periode::daterange,
                :beregning_type::utbetaling_beregning_type,
                :belop_beregnet,
                :innsender,
                :tilskuddstype::tilskuddstype,
                :beskrivelse,
                :godkjent_av_arrangor_tidspunkt
            ) on conflict (id) do update set
                gjennomforing_id = excluded.gjennomforing_id,
                kontonummer = excluded.kontonummer,
                kid = excluded.kid,
                periode = excluded.periode,
                beregning_type = excluded.beregning_type,
                belop_beregnet = excluded.belop_beregnet,
                innsender = excluded.innsender,
                tilskuddstype = excluded.tilskuddstype,
                beskrivelse = excluded.beskrivelse,
                godkjent_av_arrangor_tidspunkt = excluded.godkjent_av_arrangor_tidspunkt
        """.trimIndent()

        val params = mapOf(
            "id" to dbo.id,
            "gjennomforing_id" to dbo.gjennomforingId,
            "kontonummer" to dbo.kontonummer?.value,
            "kid" to dbo.kid?.value,
            "periode" to dbo.periode.toDaterange(),
            "beregning_type" to when (dbo.beregning) {
                is UtbetalingBeregningFri -> UtbetalingBeregningType.FRI
                is UtbetalingBeregningPrisPerManedsverk -> UtbetalingBeregningType.PRIS_PER_MANEDSVERK
                is UtbetalingBeregningPrisPerUkesverk -> UtbetalingBeregningType.PRIS_PER_UKESVERK
            }.name,
            "belop_beregnet" to dbo.beregning.output.belop,
            "innsender" to dbo.innsender?.textRepr(),
            "beskrivelse" to dbo.beskrivelse,
            "tilskuddstype" to dbo.tilskuddstype.name,
            "godkjent_av_arrangor_tidspunkt" to dbo.godkjentAvArrangorTidspunkt,
        )

        execute(queryOf(utbetalingQuery, params))

        when (dbo.beregning) {
            is UtbetalingBeregningFri -> Unit

            is UtbetalingBeregningPrisPerManedsverk -> {
                upsertUtbetalingBeregningInputSats(dbo.id, dbo.beregning.input.sats)
                upsertUtbetalingBeregningInputStengt(dbo.id, dbo.beregning.input.stengt)
                upsertUtbetalingBeregningInputDeltakelsePerioder(dbo.id, dbo.beregning.input.deltakelser)
                val deltakelser = dbo.beregning.output.deltakelser.map { it.deltakelseId to it.manedsverk }
                upsertUtbetalingBeregningOutputDeltakelseFaktor(dbo.id, deltakelser)
            }

            is UtbetalingBeregningPrisPerUkesverk -> {
                upsertUtbetalingBeregningInputSats(dbo.id, dbo.beregning.input.sats)
                upsertUtbetalingBeregningInputStengt(dbo.id, dbo.beregning.input.stengt)
                // TODO: lagre perioder uten deltakelsesprosent?
                val perioder = dbo.beregning.input.deltakelser
                    .map { DeltakelsePerioder(it.deltakelseId, listOf(DeltakelsesprosentPeriode(it.periode, 100.0))) }
                    .toSet()
                upsertUtbetalingBeregningInputDeltakelsePerioder(dbo.id, perioder)
                val deltakelser = dbo.beregning.output.deltakelser.map { it.deltakelseId to it.ukesverk }
                upsertUtbetalingBeregningOutputDeltakelseFaktor(dbo.id, deltakelser)
            }
        }
    }

    private fun Session.upsertUtbetalingBeregningInputSats(id: UUID, sats: Int) {
        @Language("PostgreSQL")
        val query = """
            insert into utbetaling_beregning_sats (utbetaling_id, sats)
            values (:utbetaling_id::uuid, :sats)
            on conflict (utbetaling_id) do update set
                sats = excluded.sats
        """.trimIndent()
        val params = mapOf(
            "utbetaling_id" to id,
            "sats" to sats,
        )
        execute(queryOf(query, params))
    }

    private fun Session.upsertUtbetalingBeregningInputStengt(id: UUID, stengt: Set<StengtPeriode>) {
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
        val stengt = stengt.map { stengt ->
            mapOf(
                "utbetaling_id" to id,
                "periode" to stengt.periode.toDaterange(),
                "beskrivelse" to stengt.beskrivelse,
            )
        }
        batchPreparedNamedStatement(insertStengtHosArrangorQuery, stengt)
    }

    private fun Session.upsertUtbetalingBeregningInputDeltakelsePerioder(id: UUID, perioder: Set<DeltakelsePerioder>) {
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
        val perioderParams = perioder.flatMap { deltakelse ->
            deltakelse.perioder.map { periode ->
                mapOf(
                    "utbetaling_id" to id,
                    "deltakelse_id" to deltakelse.deltakelseId,
                    "periode" to periode.periode.toDaterange(),
                    "deltakelsesprosent" to periode.deltakelsesprosent,
                )
            }
        }
        batchPreparedNamedStatement(insertPeriodeQuery, perioderParams)
    }

    private fun Session.upsertUtbetalingBeregningOutputDeltakelseFaktor(
        id: UUID,
        deltakelser: List<Pair<UUID, Double>>,
    ) {
        @Language("PostgreSQL")
        val deleteDeltakelseFaktor = """
            delete
            from utbetaling_deltakelse_faktor
            where utbetaling_id = ?::uuid
        """
        execute(queryOf(deleteDeltakelseFaktor, id))

        @Language("PostgreSQL")
        val insertDeltakelseFaktor = """
            insert into utbetaling_deltakelse_faktor (utbetaling_id, deltakelse_id, faktor)
            values (:utbetaling_id, :deltakelse_id, :faktor)
        """.trimIndent()

        val deltakelseFaktorParams = deltakelser.map { deltakelse ->
            mapOf(
                "utbetaling_id" to id,
                "deltakelse_id" to deltakelse.first,
                "faktor" to deltakelse.second,
            )
        }
        batchPreparedNamedStatement(insertDeltakelseFaktor, deltakelseFaktorParams)
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

    fun setBegrunnelseMindreBetalt(id: UUID, begrunnelse: String) {
        @Language("PostgreSQL")
        val query = """
            update utbetaling set
                begrunnelse_mindre_betalt = :begrunnelse
            where id = :id::uuid
        """.trimIndent()

        session.execute(queryOf(query, mapOf("id" to id, "begrunnelse" to begrunnelse)))
    }

    fun getOrError(id: UUID): Utbetaling {
        return checkNotNull(get(id)) { "Utbetaling med id $id finnes ikke" }
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
            order by periode desc
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

    fun delete(ud: UUID) {
        @Language("PostgreSQL")
        val query = """
            delete from utbetaling
            where id = ?::uuid
        """.trimIndent()

        session.execute(queryOf(query, ud))
    }

    private fun Row.toUtbetalingDto(): Utbetaling {
        val id = uuid("id")
        val beregning = getBeregning(id, UtbetalingBeregningType.valueOf(string("beregning_type")))
        val innsender = stringOrNull("innsender")?.toAgent()
        return Utbetaling(
            id = id,
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
                kid = stringOrNull("kid")?.let { Kid.parseOrThrow(it) },
            ),
            journalpostId = stringOrNull("journalpost_id"),
            periode = periode("periode"),
            innsender = innsender,
            createdAt = localDateTime("created_at"),
            beskrivelse = stringOrNull("beskrivelse"),
            begrunnelseMindreBetalt = stringOrNull("begrunnelse_mindre_betalt"),
            tilskuddstype = Tilskuddstype.valueOf(string("tilskuddstype")),
        )
    }

    private fun getBeregning(id: UUID, beregning: UtbetalingBeregningType): UtbetalingBeregning {
        return when (beregning) {
            UtbetalingBeregningType.FRI -> getBeregningFri(id)
            UtbetalingBeregningType.PRIS_PER_MANEDSVERK -> getBeregningPrisPerManedsverk(id)
            UtbetalingBeregningType.PRIS_PER_UKESVERK -> getBeregningPrisPerUkesverk(id)
        }
    }

    private fun getBeregningFri(id: UUID): UtbetalingBeregning {
        @Language("PostgreSQL")
        val query = """
            select belop_beregnet
            from utbetaling
            where id = ?::uuid
        """.trimIndent()

        return session.requireSingle(queryOf(query, id)) {
            UtbetalingBeregningFri(
                input = UtbetalingBeregningFri.Input(
                    belop = it.int("belop_beregnet"),
                ),
                output = UtbetalingBeregningFri.Output(
                    belop = it.int("belop_beregnet"),
                ),
            )
        }
    }

    private fun getBeregningPrisPerManedsverk(id: UUID): UtbetalingBeregning {
        @Language("PostgreSQL")
        val query = """
            select *
            from view_utbetaling_beregning_manedsverk
            where id = ?::uuid
        """.trimIndent()

        return session.requireSingle(queryOf(query, id)) { row ->
            UtbetalingBeregningPrisPerManedsverk(
                input = UtbetalingBeregningPrisPerManedsverk.Input(
                    periode = row.periode("periode"),
                    sats = row.int("sats"),
                    stengt = Json.decodeFromString(row.string("stengt_json")),
                    deltakelser = Json.decodeFromString(row.string("perioder_json")),
                ),
                output = UtbetalingBeregningPrisPerManedsverk.Output(
                    belop = row.int("belop_beregnet"),
                    deltakelser = Json.decodeFromString(row.string("manedsverk_json")),
                ),
            )
        }
    }

    private fun getBeregningPrisPerUkesverk(id: UUID): UtbetalingBeregningPrisPerUkesverk {
        @Language("PostgreSQL")
        val query = """
            select *
            from view_utbetaling_beregning_ukesverk
            where id = ?::uuid
        """.trimIndent()

        return session.requireSingle(queryOf(query, id)) { row ->
            UtbetalingBeregningPrisPerUkesverk(
                input = UtbetalingBeregningPrisPerUkesverk.Input(
                    periode = row.periode("periode"),
                    sats = row.int("sats"),
                    stengt = Json.decodeFromString(row.string("stengt_json")),
                    deltakelser = Json.decodeFromString(row.string("perioder_json")),
                ),
                output = UtbetalingBeregningPrisPerUkesverk.Output(
                    belop = row.int("belop_beregnet"),
                    deltakelser = Json.decodeFromString(row.string("ukesverk_json")),
                ),
            )
        }
    }
}
