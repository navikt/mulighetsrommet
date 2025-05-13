package no.nav.mulighetsrommet.api.avtale.db

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.avtale.OpsjonLoggRequest
import no.nav.mulighetsrommet.api.avtale.model.OpsjonLoggEntry
import no.nav.mulighetsrommet.model.NavIdent
import org.intellij.lang.annotations.Language
import java.util.*

class OpsjonLoggQueries(private val session: Session) {

    fun insert(entry: OpsjonLoggEntry) = with(session) {
        @Language("PostgreSQL")
        val query = """
            insert into avtale_opsjon_logg(avtale_id, sluttdato, forrige_sluttdato, status, registrert_dato, registrert_av)
            values (:avtale_id, :sluttdato, :forrige_sluttdato, :status::opsjonstatus, :registrert_dato, :registrert_av)
        """.trimIndent()

        val params = mapOf(
            "avtale_id" to entry.avtaleId,
            "sluttdato" to entry.sluttdato,
            "forrige_sluttdato" to entry.forrigeSluttdato,
            "status" to entry.status.name,
            "registrert_dato" to entry.registretDato,
            "registrert_av" to entry.registrertAv.value,
        )

        execute(queryOf(query, params))
    }

    fun delete(opsjonLoggEntryId: UUID) = with(session) {
        @Language("PostgreSQL")
        val deleteOpsjonLoggEntryQuery = """
            delete from avtale_opsjon_logg where id = ?
        """.trimIndent()

        execute(queryOf(deleteOpsjonLoggEntryQuery, opsjonLoggEntryId))
    }

    fun get(avtaleId: UUID): List<OpsjonLoggEntry> = with(session) {
        @Language("PostgreSQL")
        val getSisteOpsjonerQuery = """
            select *
            from avtale_opsjon_logg
            where avtale_id = ?::uuid
            order by registrert_dato desc
        """.trimIndent()

        return list(queryOf(getSisteOpsjonerQuery, avtaleId)) {
            it.toOpsjonLoggEntry()
        }
    }

    private fun Row.toOpsjonLoggEntry(): OpsjonLoggEntry {
        return OpsjonLoggEntry(
            avtaleId = uuid("avtale_id"),
            registretDato = localDate("registrert_dato"),
            sluttdato = localDateOrNull("sluttdato"),
            forrigeSluttdato = localDateOrNull("forrige_sluttdato"),
            status = OpsjonLoggRequest.OpsjonsLoggStatus.valueOf(string("status")),
            registrertAv = NavIdent(string("registrert_av")),
        )
    }
}
