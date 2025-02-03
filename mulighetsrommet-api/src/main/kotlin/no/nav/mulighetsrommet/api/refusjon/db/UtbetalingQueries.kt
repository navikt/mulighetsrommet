package no.nav.mulighetsrommet.api.refusjon.db

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.refusjon.model.TilsagnUtbetalingDto
import no.nav.mulighetsrommet.database.withTransaction
import no.nav.mulighetsrommet.model.NavIdent
import org.intellij.lang.annotations.Language
import java.util.*

class UtbetalingQueries(private val session: Session) {
    fun opprettTilsagnUtbetalinger(dbos: List<TilsagnUtbetalingDbo>) = withTransaction(session) {
        @Language("PostgreSQL")
        val query = """
            insert into tilsagn_utbetaling (
                tilsagn_id,
                refusjonskrav_id,
                belop,
                opprettet_av
            ) values (
                :tilsagn_id::uuid,
                :refusjonskrav_id::uuid,
                :belop,
                :opprettet_av
            );
        """.trimIndent()

        batchPreparedNamedStatement(
            query,
            dbos.map { it ->
                mapOf(
                    "tilsagn_id" to it.tilsagnId,
                    "refusjonskrav_id" to it.refusjonskravId,
                    "belop" to it.belop,
                    "opprettet_av" to it.opprettetAv.value,
                )
            },
        )
    }

    fun getByRefusjonskravId(id: UUID): List<TilsagnUtbetalingDto> = with(session) {
        @Language("PostgreSQL")
        val query = """
            select * from tilsagn_utbetaling
            where refusjonskrav_id = :id::uuid
        """.trimIndent()

        return list(queryOf(query, mapOf("id" to id))) { it.toTilsagnUtbetalingDto() }
    }
}

data class TilsagnUtbetalingDbo(
    val tilsagnId: UUID,
    val refusjonskravId: UUID,
    val opprettetAv: NavIdent,
    val belop: Int,
)

fun Row.toTilsagnUtbetalingDto(): TilsagnUtbetalingDto {
    return TilsagnUtbetalingDto(
        tilsagnId = uuid("tilsagn_id"),
        refusjonskravId = uuid("refusjonskrav_id"),
        opprettetAv = NavIdent(string("opprettet_av")),
        besluttetAv = stringOrNull("besluttet_av")?.let { NavIdent(it) },
        belop = int("belop"),
    )
}
