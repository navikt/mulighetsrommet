package no.nav.mulighetsrommet.api.utbetaling.tilskudd.db

import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.utbetaling.tilskudd.model.TilskuddUtbetalingDbo
import org.intellij.lang.annotations.Language

class TilskuddUtbetalingQueries(private val session: Session) {
    fun create(utbetaling: TilskuddUtbetalingDbo) {
        @Language("PostgreSQL")
        val query = """
            insert into tilskudd_utbetaling(id,
                id,
                vedtak_id,
                tiltakstype_id,
                tilskudd_id,
                belop,
                totrinnskontroll_id
            ) values (
                :id,
                :vedtak_id,
                :tiltakstype_id,
                :tilskudd_id,
                :belop,
                :totrinnskontroll_id
            )
        """.trimIndent()
        val params = mapOf(
            ":id" to utbetaling.id,
            ":vedtak_id" to utbetaling.vedtakId,
            ":tiltakstype_id" to utbetaling.tiltakstypeId,
            ":tilskudd_id" to utbetaling.tilskuddId,
            ":belop" to utbetaling.belop,
            ":totrinnskontroll_id" to utbetaling.totrinnskontrollId,
        )
        session.execute(queryOf(query, params))
    }
}
