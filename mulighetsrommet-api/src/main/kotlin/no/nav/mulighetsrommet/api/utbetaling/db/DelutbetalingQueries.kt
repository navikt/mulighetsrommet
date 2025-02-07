package no.nav.mulighetsrommet.api.utbetaling.db

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingDto
import no.nav.mulighetsrommet.database.utils.periode
import no.nav.mulighetsrommet.database.withTransaction
import no.nav.mulighetsrommet.model.NavIdent
import org.intellij.lang.annotations.Language
import java.util.*

class DelutbetalingQueries(private val session: Session) {
    fun opprettDelutbetalinger(delutbetalinger: List<DelutbetalingDbo>) = withTransaction(session) {
        @Language("PostgreSQL")
        val query = """
            insert into delutbetaling (
                tilsagn_id,
                utbetaling_id,
                belop,
                periode,
                opprettet_av
            ) values (
                :tilsagn_id::uuid,
                :utbetaling_id::uuid,
                :belop,
                daterange(:periode_start, :periode_slutt),
                :opprettet_av
            );
        """.trimIndent()

        val params = delutbetalinger.map {
            mapOf(
                "tilsagn_id" to it.tilsagnId,
                "utbetaling_id" to it.utbetalingId,
                "belop" to it.belop,
                "periode_start" to it.periode.start,
                "periode_slutt" to it.periode.slutt,
                "opprettet_av" to it.opprettetAv.value,
            )
        }
        batchPreparedNamedStatement(query, params)
    }

    fun getByUtbetalingId(id: UUID): List<DelutbetalingDto> = with(session) {
        @Language("PostgreSQL")
        val query = """
            select
                tilsagn_id,
                utbetaling_id,
                belop,
                periode,
                opprettet_av,
                besluttet_av
            from delutbetaling
            where utbetaling_id = ?
        """.trimIndent()

        return list(queryOf(query, id)) { it.toDelutbetalingDto() }
    }
}

private fun Row.toDelutbetalingDto(): DelutbetalingDto {
    val besluttetAv = stringOrNull("besluttet_av")?.let { NavIdent(it) }

    return when (besluttetAv) {
        null -> DelutbetalingDto.DelutbetalingTilGodkjenning(
            tilsagnId = uuid("tilsagn_id"),
            utbetalingId = uuid("utbetaling_id"),
            opprettetAv = NavIdent(string("opprettet_av")),
            belop = int("belop"),
            periode = periode("periode"),
        )

        else -> DelutbetalingDto.DelutbetalingGodkjent(
            tilsagnId = uuid("tilsagn_id"),
            utbetalingId = uuid("utbetaling_id"),
            belop = int("belop"),
            periode = periode("periode"),
            opprettetAv = NavIdent(string("opprettet_av")),
            besluttetAv = besluttetAv,
        )
    }
}
