package no.nav.mulighetsrommet.api.utbetaling.db

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingDto
import no.nav.mulighetsrommet.database.withTransaction
import no.nav.mulighetsrommet.model.NavIdent
import org.intellij.lang.annotations.Language
import java.util.*

class DelutbetalingQueries(private val session: Session) {
    fun opprettTilsagnUtbetalinger(dbos: List<TilsagnUtbetalingDbo>) = withTransaction(session) {
        @Language("PostgreSQL")
        val query = """
            insert into delutbetaling (
                tilsagn_id,
                utbetaling_id,
                belop,
                opprettet_av
            ) values (
                :tilsagn_id::uuid,
                :utbetaling_id::uuid,
                :belop,
                :opprettet_av
            );
        """.trimIndent()

        batchPreparedNamedStatement(
            query,
            dbos.map { it ->
                mapOf(
                    "tilsagn_id" to it.tilsagnId,
                    "utbetaling_id" to it.utbetalingId,
                    "belop" to it.belop,
                    "opprettet_av" to it.opprettetAv.value,
                )
            },
        )
    }

    fun getByutbetalingId(id: UUID): List<DelutbetalingDto> = with(session) {
        @Language("PostgreSQL")
        val query = """
            select * from delutbetaling
            where utbetaling_id = :id::uuid
        """.trimIndent()

        return list(queryOf(query, mapOf("id" to id))) { it.toTilsagnUtbetalingDto() }
    }
}

data class TilsagnUtbetalingDbo(
    val tilsagnId: UUID,
    val utbetalingId: UUID,
    val opprettetAv: NavIdent,
    val belop: Int,
)

fun Row.toTilsagnUtbetalingDto(): DelutbetalingDto {
    val besluttetAv = stringOrNull("besluttet_av")?.let { NavIdent(it) }

    return when (besluttetAv) {
        null -> DelutbetalingDto.DelutbetalingTilGodkjenning(
            tilsagnId = uuid("tilsagn_id"),
            utbetalingId = uuid("utbetaling_id"),
            opprettetAv = NavIdent(string("opprettet_av")),
            belop = int("belop"),
        )
        else -> DelutbetalingDto.DelutbetalingGodkjent(
            tilsagnId = uuid("tilsagn_id"),
            utbetalingId = uuid("utbetaling_id"),
            opprettetAv = NavIdent(string("opprettet_av")),
            besluttetAv = besluttetAv,
            belop = int("belop"),
        )
    }
}
