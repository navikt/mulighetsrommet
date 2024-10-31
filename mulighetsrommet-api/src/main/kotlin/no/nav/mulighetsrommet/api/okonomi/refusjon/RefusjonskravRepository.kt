package no.nav.mulighetsrommet.api.okonomi.refusjon

import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.Session
import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.okonomi.models.RefusjonKravBeregning
import no.nav.mulighetsrommet.api.okonomi.models.RefusjonKravBeregningAft
import no.nav.mulighetsrommet.api.okonomi.models.RefusjonskravDto
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.domain.dto.Kid
import no.nav.mulighetsrommet.domain.dto.Kontonummer
import no.nav.mulighetsrommet.domain.dto.Organisasjonsnummer
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime
import java.util.*

class RefusjonskravRepository(private val db: Database) {
    fun upsert(dbo: RefusjonskravDbo) = db.transaction {
        upsert(dbo, it)
    }

    fun upsert(dbo: RefusjonskravDbo, tx: TransactionalSession) {
        @Language("PostgreSQL")
        val refusjonskravQuery = """
            insert into refusjonskrav (id, gjennomforing_id, frist_for_godkjenning, kontonummer, kid)
            values (:id::uuid, :gjennomforing_id::uuid, :frist_for_godkjenning, :kontonummer, :kid)
            on conflict (id) do update set
                gjennomforing_id = excluded.gjennomforing_id,
                frist_for_godkjenning = excluded.frist_for_godkjenning,
                kontonummer = excluded.kontonummer,
                kid = excluded.kid
        """.trimIndent()

        val params = mapOf(
            "id" to dbo.id,
            "gjennomforing_id" to dbo.gjennomforingId,
            "frist_for_godkjenning" to dbo.fristForGodkjenning,
            "kontonummer" to dbo.kontonummer?.value,
            "kid" to dbo.kid?.value,
        )

        queryOf(refusjonskravQuery, params).asExecute.runWithSession(tx)

        when (dbo.beregning) {
            is RefusjonKravBeregningAft -> {
                upsertRefusjonskravBeregningAft(tx, dbo.id, dbo.beregning)
            }
        }
    }

    private fun upsertRefusjonskravBeregningAft(
        tx: TransactionalSession,
        id: UUID,
        beregning: RefusjonKravBeregningAft,
    ) {
        @Language("PostgreSQL")
        val query = """
            insert into refusjonskrav_beregning_aft (refusjonskrav_id, periode, sats, belop)
            values (:refusjonskrav_id::uuid, tsrange(:periode_start, :periode_slutt), :sats, :belop)
            on conflict (refusjonskrav_id) do update set
                periode = excluded.periode,
                sats = excluded.sats,
                belop = excluded.belop
        """.trimIndent()

        val params = mapOf(
            "refusjonskrav_id" to id,
            "periode_start" to beregning.input.periodeStart,
            "periode_slutt" to beregning.input.periodeSlutt,
            "sats" to beregning.input.sats,
            "belop" to beregning.output.belop,
        )
        queryOf(query, params).asExecute.runWithSession(tx)

        @Language("PostgreSQL")
        val deletePerioderQuery = """
            delete
            from refusjonskrav_deltakelse_periode
            where refusjonskrav_id = :refusjonskrav_id::uuid;
        """
        queryOf(deletePerioderQuery, mapOf("refusjonskrav_id" to id)).asExecute.runWithSession(tx)

        @Language("PostgreSQL")
        val insertPeriodeQuery = """
            insert into refusjonskrav_deltakelse_periode (refusjonskrav_id, deltakelse_id, periode, prosent_stilling)
            values (:refusjonskrav_id, :deltakelse_id, tsrange(:start, :slutt), :stillingsprosent)
        """.trimIndent()

        val perioder = beregning.input.deltakelser.flatMap { deltakelse ->
            deltakelse.perioder.map { periode ->
                mapOf(
                    "refusjonskrav_id" to id,
                    "deltakelse_id" to deltakelse.deltakelseId,
                    "start" to periode.start,
                    "slutt" to periode.slutt,
                    "stillingsprosent" to periode.stillingsprosent,
                )
            }
        }
        tx.batchPreparedNamedStatement(insertPeriodeQuery, perioder)

        @Language("PostgreSQL")
        val deleteManedsverk = """
            delete
            from refusjonskrav_deltakelse_manedsverk
            where refusjonskrav_id = :refusjonskrav_id::uuid;
        """
        queryOf(deleteManedsverk, mapOf("refusjonskrav_id" to id)).asExecute.runWithSession(tx)

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
        tx.batchPreparedNamedStatement(insertManedsverkQuery, manedsverk)
    }

    fun setGodkjentAvArrangor(id: UUID, tidspunkt: LocalDateTime) {
        @Language("PostgreSQL")
        val query = """
            update refusjonskrav
            set godkjent_av_arrangor_tidspunkt = :tidspunkt
            where id = :id::uuid
        """.trimIndent()

        queryOf(query, mapOf("id" to id, "tidspunkt" to tidspunkt))
            .asUpdate
            .let { db.run(it) }
    }

    fun setBetalingsInformasjon(id: UUID, kontonummer: Kontonummer, kid: Kid?) {
        @Language("PostgreSQL")
        val query = """
            update refusjonskrav
            set kontonummer = :kontonummer, kid = :kid
            where id = :id::uuid
        """.trimIndent()

        queryOf(
            query,
            mapOf(
                "id" to id,
                "kontonummer" to kontonummer.value,
                "kid" to kid?.value,
            ),
        )
            .asUpdate
            .let { db.run(it) }
    }

    fun get(id: UUID) = db.transaction { get(id, it) }

    fun get(id: UUID, tx: Session): RefusjonskravDto? {
        @Language("PostgreSQL")
        val refusjonskravQuery = """
            select *
            from refusjonskrav_aft_view
            where id = :id::uuid
        """.trimIndent()

        return queryOf(refusjonskravQuery, mapOf("id" to id))
            .map { it.toRefusjonsKravAft() }
            .asSingle
            .runWithSession(tx)
    }

    fun getByArrangorIds(organisasjonsnummer: Organisasjonsnummer): List<RefusjonskravDto> =
        db.transaction { getByArrangorIds(organisasjonsnummer, it) }

    fun getByArrangorIds(
        organisasjonsnummer: Organisasjonsnummer,
        tx: Session,
    ): List<RefusjonskravDto> {
        @Language("PostgreSQL")
        val query = """
            select * from refusjonskrav_aft_view
            where arrangor_organisasjonsnummer = :organisasjonsnummer
        """.trimIndent()

        return tx.run(
            queryOf(query, mapOf("organisasjonsnummer" to organisasjonsnummer.value))
                .map { it.toRefusjonsKravAft() }
                .asList,
        )
    }

    fun getByGjennomforing(id: UUID, status: RefusjonskravStatus): List<RefusjonskravDto> = db.useSession {
        getByGjennomforing(it, id, status)
    }

    fun getByGjennomforing(tx: Session, id: UUID, status: RefusjonskravStatus): List<RefusjonskravDto> {
        @Language("PostgreSQL")
        val query = """
            select *
            from refusjonskrav_aft_view
            where gjennomforing_id = :id::uuid and status = :status::refusjonskrav_status
        """.trimIndent()

        val params = mapOf("id" to id, "status" to status.name)

        return queryOf(query, params)
            .map { it.toRefusjonsKravAft() }
            .asList
            .runWithSession(tx)
    }

    private fun Row.toRefusjonsKravAft(): RefusjonskravDto {
        val beregning = RefusjonKravBeregningAft(
            input = RefusjonKravBeregningAft.Input(
                periodeStart = localDateTime("periode_start"),
                periodeSlutt = localDateTime("periode_slutt"),
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
        )
    }
}
