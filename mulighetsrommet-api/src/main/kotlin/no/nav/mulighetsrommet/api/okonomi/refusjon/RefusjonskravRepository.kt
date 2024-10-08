package no.nav.mulighetsrommet.api.okonomi.refusjon

import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.okonomi.prismodell.Prismodell
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.domain.dto.Organisasjonsnummer
import org.intellij.lang.annotations.Language
import java.util.*

class RefusjonskravRepository(private val db: Database) {
    fun upsert(dbo: RefusjonskravDbo) =
        db.transaction { upsert(dbo, it) }

    fun upsert(dbo: RefusjonskravDbo, tx: Session) {
        @Language("PostgreSQL")
        val refusjonskravQuery = """
            insert into refusjonskrav (
                id,
                tiltaksgjennomforing_id,
                periode_start,
                periode_slutt
            ) values (
                :id::uuid,
                :tiltaksgjennomforing_id::uuid,
                :periode_start,
                :periode_slutt
            )
            on conflict (id) do update set
                tiltaksgjennomforing_id = excluded.tiltaksgjennomforing_id,
                periode_start           = excluded.periode_start,
                periode_slutt           = excluded.periode_slutt
        """.trimIndent()

        queryOf(refusjonskravQuery, dbo.toSqlParameters()).asExecute.runWithSession(tx)

        when (dbo.beregning) {
            is Prismodell.RefusjonskravBeregning.AFT -> {
                upsertRefusjonskravBeregningAft(tx, dbo.id, dbo.beregning)
            }

            else -> throw IllegalStateException("Refusjonskrav av typen ${dbo.beregning::class} er ikke støtte enda")
        }
    }

    private fun upsertRefusjonskravBeregningAft(
        tx: Session,
        id: UUID,
        beregning: Prismodell.RefusjonskravBeregning.AFT,
    ) {
        @Language("PostgreSQL")
        val query = """
            insert into refusjonskrav_beregning_aft (refusjonskrav_id, belop, sats)
            values (:refusjonskrav_id::uuid, :belop, :sats)
            on conflict (refusjonskrav_id) do update set
                belop = excluded.belop,
                sats  = excluded.sats
        """.trimIndent()

        val params = mapOf(
            "refusjonskrav_id" to id,
            "belop" to beregning.belop,
            "sats" to beregning.sats,
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

        val perioder = beregning.deltakere.flatMap { deltakelse ->
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
    }

    fun get(id: UUID) = db.transaction { get(id, it) }

    fun get(id: UUID, tx: Session): RefusjonskravDto? {
        @Language("PostgreSQL")
        val refusjonskravQuery = """
            select * from refusjonskrav_aft_view
            where id = :id::uuid
        """.trimIndent()

        return queryOf(refusjonskravQuery, mapOf("id" to id))
            .map { it.toRefusjonskravAftDto() }
            .asSingle
            .runWithSession(tx)
    }

    fun getByOrgnr(orgnr: List<Organisasjonsnummer>) = db.transaction { getByOrgnr(orgnr, it) }

    fun getByOrgnr(orgnr: List<Organisasjonsnummer>, tx: Session): List<RefusjonskravDto> {
        @Language("PostgreSQL")
        val query = """
            select * from refusjonskrav_aft_view
            where arrangor_organisasjonsnummer = any(:orgnr)
        """.trimIndent()

        return tx.run(
            queryOf(query, mapOf("orgnr" to db.createTextArray(orgnr.map { it.value })))
                .map { it.toRefusjonskravAftDto() }
                .asList,
        )
    }

    fun getByGjennomforingId(id: UUID) = db.transaction { getByGjennomforingId(id, it) }

    fun getByGjennomforingId(id: UUID, tx: Session): RefusjonskravDto? {
        @Language("PostgreSQL")
        val query = """
            select * from refusjonskrav_aft_view
            where tiltaksgjennomforing_id = :id::uuid
        """.trimIndent()

        return queryOf(query, mapOf("id" to id))
            .map { it.toRefusjonskravAftDto() }
            .asSingle
            .runWithSession(tx)
    }

    private fun RefusjonskravDbo.toSqlParameters() = mapOf(
        "id" to id,
        "tiltaksgjennomforing_id" to tiltaksgjennomforingId,
        "periode_start" to periodeStart,
        "periode_slutt" to periodeSlutt,
    )

    private fun Row.toRefusjonskravAftDto(): RefusjonskravDto {
        val beregning = Prismodell.RefusjonskravBeregning.AFT(
            belop = int("belop"),
            sats = int("sats"),
            deltakere = stringOrNull("deltakelser_json")?.let { Json.decodeFromString(it) } ?: setOf(),
        )
        return toRefusjonskravDto(beregning)
    }

    private fun Row.toRefusjonskravDto(beregning: Prismodell.RefusjonskravBeregning): RefusjonskravDto {
        return RefusjonskravDto(
            id = uuid("id"),
            tiltaksgjennomforing = RefusjonskravDto.Gjennomforing(
                id = uuid("tiltaksgjennomforing_id"),
                navn = string("tiltaksgjennomforing_navn"),
            ),
            periodeStart = localDate("periode_start"),
            periodeSlutt = localDate("periode_slutt"),
            arrangor = RefusjonskravDto.Arrangor(
                id = uuid("arrangor_id"),
                organisasjonsnummer = string("arrangor_organisasjonsnummer"),
                navn = string("arrangor_navn"),
                slettet = boolean("arrangor_slettet"),
            ),
            tiltakstype = RefusjonskravDto.Tiltakstype(
                navn = string("tiltakstype_navn"),
            ),
            beregning = beregning,
        )
    }
}
