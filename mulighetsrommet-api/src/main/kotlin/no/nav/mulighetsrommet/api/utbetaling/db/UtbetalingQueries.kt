package no.nav.mulighetsrommet.api.utbetaling.db

import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.Session
import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.tiltakstype.db.createArrayOfTiltakskode
import no.nav.mulighetsrommet.api.utbetaling.model.*
import no.nav.mulighetsrommet.database.createTextArray
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
                godkjent_av_arrangor_tidspunkt,
                status,
                datastream_periode_start,
                datastream_periode_slutt
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
                :godkjent_av_arrangor_tidspunkt,
                :status::utbetaling_status,
                :datastream_periode_start::date,
                :datastream_periode_slutt::date
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
                godkjent_av_arrangor_tidspunkt = excluded.godkjent_av_arrangor_tidspunkt,
                status = excluded.status,
                datastream_periode_start      = excluded.datastream_periode_start,
                datastream_periode_slutt      = excluded.datastream_periode_slutt
        """.trimIndent()

        val params = mapOf(
            "id" to dbo.id,
            "gjennomforing_id" to dbo.gjennomforingId,
            "kontonummer" to dbo.kontonummer?.value,
            "kid" to dbo.kid?.value,
            "periode" to dbo.periode.toDaterange(),
            "beregning_type" to when (dbo.beregning) {
                is UtbetalingBeregningFri -> UtbetalingBeregningType.FRI
                is UtbetalingBeregningFastSatsPerTiltaksplassPerManed -> UtbetalingBeregningType.FAST_SATS_PER_TILTAKSPLASS_PER_MANED
                is UtbetalingBeregningPrisPerManedsverk -> UtbetalingBeregningType.PRIS_PER_MANEDSVERK
                is UtbetalingBeregningPrisPerUkesverk -> UtbetalingBeregningType.PRIS_PER_UKESVERK
            }.name,
            "belop_beregnet" to dbo.beregning.output.belop,
            "innsender" to dbo.innsender?.textRepr(),
            "beskrivelse" to dbo.beskrivelse,
            "tilskuddstype" to dbo.tilskuddstype.name,
            "godkjent_av_arrangor_tidspunkt" to dbo.godkjentAvArrangorTidspunkt,
            "status" to dbo.status.name,
            "datastream_periode_start" to dbo.periode.start,
            "datastream_periode_slutt" to dbo.periode.getLastInclusiveDate(),
        )

        execute(queryOf(utbetalingQuery, params))

        when (dbo.beregning) {
            is UtbetalingBeregningFri -> Unit

            is UtbetalingBeregningFastSatsPerTiltaksplassPerManed -> {
                upsertUtbetalingBeregningInputSats(dbo.id, dbo.beregning.input.sats)
                upsertUtbetalingBeregningInputStengt(dbo.id, dbo.beregning.input.stengt)
                upsertUtbetalingBeregningInputDeltakelsePerioder(dbo.id, dbo.beregning.input.deltakelser)
                upsertUtbetalingBeregningOutputDeltakelseFaktor(dbo.id, dbo.beregning.output.deltakelser)
            }

            is UtbetalingBeregningPrisPerManedsverk -> upsertBeregning(
                dbo.id,
                dbo.beregning.input.sats,
                dbo.beregning.input.stengt,
                dbo.beregning.input.deltakelser,
                dbo.beregning.output.deltakelser,
            )

            is UtbetalingBeregningPrisPerUkesverk -> upsertBeregning(
                dbo.id,
                dbo.beregning.input.sats,
                dbo.beregning.input.stengt,
                dbo.beregning.input.deltakelser,
                dbo.beregning.output.deltakelser,
            )
        }
    }

    private fun TransactionalSession.upsertBeregning(
        utbetalingId: UUID,
        sats: Int,
        stengtPerioder: Set<StengtPeriode>,
        deltakelserPeriode: Set<DeltakelsePeriode>,
        deltakelserFaktor: Set<UtbetalingBeregningOutputDeltakelse>,
    ) {
        upsertUtbetalingBeregningInputSats(utbetalingId, sats)
        upsertUtbetalingBeregningInputStengt(utbetalingId, stengtPerioder)
        // TODO: lagre perioder uten deltakelsesprosent?
        val perioder = deltakelserPeriode
            .map {
                DeltakelseDeltakelsesprosentPerioder(
                    it.deltakelseId,
                    listOf(DeltakelsesprosentPeriode(it.periode, 100.0)),
                )
            }
            .toSet()
        upsertUtbetalingBeregningInputDeltakelsePerioder(utbetalingId, perioder)
        upsertUtbetalingBeregningOutputDeltakelseFaktor(utbetalingId, deltakelserFaktor)
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
        val perioder = stengt.map {
            mapOf(
                "utbetaling_id" to id,
                "periode" to it.periode.toDaterange(),
                "beskrivelse" to it.beskrivelse,
            )
        }
        batchPreparedNamedStatement(insertStengtHosArrangorQuery, perioder)
    }

    private fun Session.upsertUtbetalingBeregningInputDeltakelsePerioder(
        id: UUID,
        perioder: Set<DeltakelseDeltakelsesprosentPerioder>,
    ) {
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
        deltakelser: Set<UtbetalingBeregningOutputDeltakelse>,
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

        val deltakelseFaktorParams = deltakelser.map {
            mapOf(
                "utbetaling_id" to id,
                "deltakelse_id" to it.deltakelseId,
                "faktor" to it.faktor,
            )
        }
        batchPreparedNamedStatement(insertDeltakelseFaktor, deltakelseFaktorParams)
    }

    fun setStatus(id: UUID, status: UtbetalingStatusType) {
        @Language("PostgreSQL")
        val query = """
            update utbetaling set
                status = :status::utbetaling_status
            where id = :id::uuid
        """.trimIndent()

        session.execute(queryOf(query, mapOf("id" to id, "status" to status.name)))
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

    fun setAvbrutt(id: UUID, tidspunkt: LocalDateTime, aarsaker: List<String>, forklaring: String?) {
        @Language("PostgreSQL")
        val query = """
            update utbetaling set
                status = 'AVBRUTT',
                avbrutt_aarsaker = :aarsaker,
                avbrutt_forklaring = :forklaring,
                avbrutt_tidspunkt = :tidspunkt
            where id = :id::uuid
        """.trimIndent()
        val params = mapOf(
            "id" to id,
            "tidspunkt" to tidspunkt,
            "aarsaker" to aarsaker.let { session.createTextArray(it) },
            "forklaring" to forklaring,
        )

        session.execute(queryOf(query, params))
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

        return session.single(queryOf(utbetalingQuery, id)) { it.toUtbetaling() }
    }

    fun getOppgaveData(tiltakskoder: Set<Tiltakskode>?): List<Utbetaling> {
        @Language("PostgreSQL")
        val utbetalingQuery = """
            select * from utbetaling_dto_view
            where (:tiltakskoder::tiltakskode[] is null or tiltakskode = any(:tiltakskoder::tiltakskode[]))
        """.trimIndent()

        val params = mapOf(
            "tiltakskoder" to tiltakskoder?.let { session.createArrayOfTiltakskode(it) },
        )

        return session.list(queryOf(utbetalingQuery, params)) { it.toUtbetaling() }
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

        return session.list(queryOf(query, organisasjonsnummer.value)) { it.toUtbetaling() }
    }

    fun getByGjennomforing(gjennomforingId: UUID): List<Utbetaling> = with(session) {
        @Language("PostgreSQL")
        val query = """
            select *
            from utbetaling_dto_view
            where gjennomforing_id = :id::uuid
        """.trimIndent()

        val params = mapOf("id" to gjennomforingId)

        return list(queryOf(query, params)) { it.toUtbetaling() }
    }

    fun getByPeriode(periode: Periode): List<Utbetaling> {
        @Language("PostgreSQL")
        val query = """
            select *
            from utbetaling_dto_view
            where periode = :periode::daterange
        """.trimIndent()

        val params = mapOf("periode" to periode.toDaterange())

        return session.list(queryOf(query, params)) { it.toUtbetaling() }
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
            it.toUtbetaling()
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

    private fun Row.toUtbetaling(): Utbetaling {
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
            status = UtbetalingStatusType.valueOf(string("status")),
        )
    }

    private fun getBeregning(id: UUID, beregning: UtbetalingBeregningType): UtbetalingBeregning {
        return when (beregning) {
            UtbetalingBeregningType.FRI -> getBeregningFri(id)
            UtbetalingBeregningType.FAST_SATS_PER_TILTAKSPLASS_PER_MANED -> {
                getBeregningPrisPerManedsverkMedDeltakelsesmengder(id)
            }

            UtbetalingBeregningType.PRIS_PER_MANEDSVERK -> getBeregningPrisPerManedsverk(id)
            UtbetalingBeregningType.PRIS_PER_UKESVERK -> getBeregningPrisPerUkesverk(id)
        }
    }

    private fun getBeregningFri(id: UUID): UtbetalingBeregning {
        @Language("PostgreSQL")
        val query = """
            select *
            from view_utbetaling_beregning_fri
            where id = ?::uuid
        """.trimIndent()

        return session.requireSingle(queryOf(query, id)) { row ->
            UtbetalingBeregningFri(
                input = UtbetalingBeregningFri.Input(
                    belop = row.int("belop_beregnet"),
                ),
                output = UtbetalingBeregningFri.Output(
                    belop = row.int("belop_beregnet"),
                ),
            )
        }
    }

    private fun getBeregningPrisPerManedsverkMedDeltakelsesmengder(id: UUID): UtbetalingBeregning {
        @Language("PostgreSQL")
        val query = """
            select *
            from view_utbetaling_beregning_manedsverk_med_deltakelsesmengder
            where id = ?::uuid
        """.trimIndent()

        return session.requireSingle(queryOf(query, id)) { row ->
            UtbetalingBeregningFastSatsPerTiltaksplassPerManed(
                input = UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Input(
                    periode = row.periode("periode"),
                    sats = row.int("sats"),
                    stengt = Json.decodeFromString(row.string("stengt_perioder_json")),
                    deltakelser = Json.decodeFromString(row.string("deltakelser_perioder_json")),
                ),
                output = UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Output(
                    belop = row.int("belop_beregnet"),
                    deltakelser = Json.decodeFromString(row.string("manedsverk_json")),
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
                    stengt = Json.decodeFromString(row.string("stengt_perioder_json")),
                    deltakelser = Json.decodeFromString(row.string("deltakelser_perioder_json")),
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
                    stengt = Json.decodeFromString(row.string("stengt_perioder_json")),
                    deltakelser = Json.decodeFromString(row.string("deltakelser_perioder_json")),
                ),
                output = UtbetalingBeregningPrisPerUkesverk.Output(
                    belop = row.int("belop_beregnet"),
                    deltakelser = Json.decodeFromString(row.string("ukesverk_json")),
                ),
            )
        }
    }
}
