package no.nav.mulighetsrommet.api.okonomi.refusjon

import kotlinx.serialization.encodeToString
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
        val query = """
            insert into refusjonskrav (
                id,
                tiltaksgjennomforing_id,
                periode_start,
                periode_slutt,
                arrangor_id,
                beregning
            ) values (
                :id::uuid,
                :tiltaksgjennomforing_id::uuid,
                :periode_start,
                :periode_slutt,
                :arrangor_id::uuid,
                :beregning::jsonb
            )
            on conflict (id) do update set
                tiltaksgjennomforing_id = excluded.tiltaksgjennomforing_id,
                periode_start           = excluded.periode_start,
                periode_slutt           = excluded.periode_slutt,
                arrangor_id             = excluded.arrangor_id,
                beregning               = excluded.beregning
            returning *
        """.trimIndent()

        tx.run(queryOf(query, dbo.toSqlParameters()).asExecute)
    }

    fun get(id: UUID) = db.transaction { get(id, it) }

    fun get(id: UUID, tx: Session): RefusjonskravDto? {
        @Language("PostgreSQL")
        val query = """
            select * from refusjonskrav_admin_dto_view
            where id = :id::uuid
        """.trimIndent()

        return tx.run(
            queryOf(query, mapOf("id" to id))
                .map { it.toRefusjonskravDto() }
                .asSingle,
        )
    }

    fun getByOrgnr(orgnr: List<Organisasjonsnummer>) = db.transaction { getByOrgnr(orgnr, it) }

    fun getByOrgnr(orgnr: List<Organisasjonsnummer>, tx: Session): List<RefusjonskravDto> {
        @Language("PostgreSQL")
        val query = """
            select * from refusjonskrav_admin_dto_view
            where arrangor_organisasjonsnummer = any(:orgnr)
        """.trimIndent()

        return tx.run(
            queryOf(query, mapOf("orgnr" to db.createTextArray(orgnr.map { it.value })))
                .map { it.toRefusjonskravDto() }
                .asList,
        )
    }

    fun getByGjennomforingId(id: UUID) = db.transaction { getByGjennomforingId(id, it) }

    fun getByGjennomforingId(id: UUID, tx: Session): RefusjonskravDto? {
        @Language("PostgreSQL")
        val query = """
            select * from refusjonskrav_admin_dto_view
            where tiltaksgjennomforing_id = :id::uuid
        """.trimIndent()

        return tx.run(
            queryOf(query, mapOf("id" to id))
                .map { it.toRefusjonskravDto() }
                .asSingle,
        )
    }

    private fun RefusjonskravDbo.toSqlParameters() = mapOf(
        "id" to id,
        "tiltaksgjennomforing_id" to tiltaksgjennomforingId,
        "periode_start" to periodeStart,
        "periode_slutt" to periodeSlutt,
        "arrangor_id" to arrangorId,
        "beregning" to Json.encodeToString(beregning),
    )

    private fun Row.toRefusjonskravDto(): RefusjonskravDto {
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
            beregning = Json.decodeFromString<Prismodell.RefusjonskravBeregning>(string("beregning")),
        )
    }
}
