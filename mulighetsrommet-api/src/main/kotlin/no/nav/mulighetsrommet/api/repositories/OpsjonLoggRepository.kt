package no.nav.mulighetsrommet.api.repositories

import io.ktor.server.plugins.*
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.domain.dto.OpsjonLoggEntry
import no.nav.mulighetsrommet.api.routes.v1.OpsjonLoggRequest
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.query
import no.nav.mulighetsrommet.domain.dto.NavIdent
import org.intellij.lang.annotations.Language
import java.util.*

class OpsjonLoggRepository(private val db: Database) {

    fun insert(entry: OpsjonLoggEntry, tx: Session) = query {
        @Language("PostgreSQL")
        val query = """
            insert into avtale_opsjon_logg(avtale_id, sluttdato, status, registrert_av)
            values (:avtaleId, :sluttdato, :status::opsjonstatus, :registrertAv)
        """.trimIndent()
        queryOf(
            query,
            mapOf(
                "avtaleId" to entry.avtaleId,
                "sluttdato" to entry.sluttdato,
                "status" to entry.status.name,
                "registrertAv" to entry.registrertAv.value,
            ),
        ).asExecute.let { tx.run(it) }
    }

    fun insert(entry: OpsjonLoggEntry) = db.transaction { insert(entry, it) }

    fun delete(opsjonLoggEntryId: UUID, tx: Session) = query {
        @Language("PostgreSQL")
        val deleteOpsjonLoggEntryQuery = """
            delete from avtale_opsjon_logg where id = :id
        """.trimIndent()
        queryOf(deleteOpsjonLoggEntryQuery, mapOf("id" to opsjonLoggEntryId)).asExecute.let { tx.run(it) }
    }

    fun getOpsjoner(avtaleId: UUID): List<OpsjonLoggEntry> {
        @Language("PostgreSQL")
        val getSisteOpsjonerQuery = """
            select * from avtale_opsjon_logg
            where avtale_id = :avtaleId::uuid and status = 'OPSJON_UTLØST'
            order by registrert_dato desc
        """.trimIndent()

        val opsjoner = queryOf(getSisteOpsjonerQuery, mapOf("avtaleId" to avtaleId)).map {
            it.toOpsjonLoggEntry()
        }.asList.let { db.run(it) }

        if (opsjoner.isEmpty()) {
            throw NotFoundException("Fant ingen utløste opsjoner for avtale med id '$avtaleId'")
        } else {
            return opsjoner
        }
    }

    private fun Row.toOpsjonLoggEntry(): OpsjonLoggEntry {
        return OpsjonLoggEntry(
            avtaleId = this.uuid("avtale_id"),
            sluttdato = this.localDate("sluttdato"),
            status = OpsjonLoggRequest.OpsjonsLoggStatus.valueOf(this.string("status")),
            registrertAv = NavIdent(this.string("registrert_av")),
        )
    }
}
