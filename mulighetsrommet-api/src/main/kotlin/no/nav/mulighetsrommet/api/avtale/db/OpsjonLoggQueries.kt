package no.nav.mulighetsrommet.api.avtale.db

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.avtale.model.AvtaleDto
import no.nav.mulighetsrommet.api.avtale.model.OpsjonLoggDbo
import no.nav.mulighetsrommet.api.avtale.model.OpsjonLoggStatus
import org.intellij.lang.annotations.Language
import java.util.*

class OpsjonLoggQueries(private val session: Session) {
    fun insert(entry: OpsjonLoggDbo) = with(session) {
        @Language("PostgreSQL")
        val query = """
            insert into avtale_opsjon_logg(avtale_id, sluttdato, forrige_sluttdato, status, registrert_av)
            values (:avtale_id, :sluttdato, :forrige_sluttdato, :status::opsjonstatus, :registrert_av)
        """.trimIndent()

        val params = mapOf(
            "avtale_id" to entry.avtaleId,
            "sluttdato" to entry.sluttDato,
            "forrige_sluttdato" to entry.forrigeSluttDato,
            "status" to entry.status.name,
            "registrert_av" to entry.registrertAv.value,
        )

        execute(queryOf(query, params))
    }

    fun delete(id: UUID) = with(session) {
        @Language("PostgreSQL")
        val query = """
            delete from avtale_opsjon_logg where id = ?
        """.trimIndent()

        execute(queryOf(query, id))
    }

    fun getByAvtaleId(avtaleId: UUID): List<AvtaleDto.OpsjonLoggDto> = with(session) {
        @Language("PostgreSQL")
        val query = """
            select *
            from avtale_opsjon_logg
            where avtale_id = ?::uuid
            order by created_at desc
        """.trimIndent()

        return list(queryOf(query, avtaleId)) {
            it.toOpsjonLoggEntry()
        }
    }

    private fun Row.toOpsjonLoggEntry(): AvtaleDto.OpsjonLoggDto {
        return AvtaleDto.OpsjonLoggDto(
            id = uuid("id"),
            createdAt = localDateTime("created_at"),
            sluttDato = localDateOrNull("sluttdato"),
            forrigeSluttDato = localDate("forrige_sluttdato"),
            status = OpsjonLoggStatus.valueOf(string("status")),
        )
    }
}
