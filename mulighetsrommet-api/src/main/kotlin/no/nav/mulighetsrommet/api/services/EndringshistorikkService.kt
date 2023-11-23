package no.nav.mulighetsrommet.api.services

import kotlinx.serialization.json.JsonElement
import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.domain.dto.EndringshistorikkDto
import no.nav.mulighetsrommet.database.Database
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime
import java.util.*

class EndringshistorikkService(
    private val db: Database,
) {

    fun getEndringshistorikk(documentClass: DocumentClass, id: UUID): EndringshistorikkDto {
        @Language("PostgreSQL")
        val statement = """
            select document_id,
                   operation,
                   lower(sys_period) as edited_at,
                   case
                       when na.nav_ident is not null then concat(na.fornavn, ' ', na.etternavn)
                       else user_id
                       end           as edited_by
            from ${documentClass.table}
                     left join nav_ansatt na on user_id = na.nav_ident
            where document_id = :document_id
            order by sys_period desc;
        """.trimIndent()

        val params = mapOf(
            "document_id" to id,
        )

        val entries = queryOf(statement, params)
            .map {
                EndringshistorikkDto.Entry(
                    id = it.uuid("document_id"),
                    operation = it.string("operation"),
                    editedAt = it.localDateTime("edited_at"),
                    editedBy = it.string("edited_by"),
                )
            }
            .asList
            .let { db.run(it) }

        return EndringshistorikkDto(entries = entries)
    }


    fun logEndring(
        documentClass: DocumentClass,
        operation: String,
        userId: String,
        documentId: UUID,
        timestamp: LocalDateTime? = null,
        valueProvider: () -> JsonElement,
    ) = db.transaction {
        logEndring(it, documentClass, operation, userId, documentId, timestamp, valueProvider)
    }

    fun logEndring(
        tx: TransactionalSession,
        documentClass: DocumentClass,
        operation: String,
        userId: String,
        documentId: UUID,
        timestamp: LocalDateTime? = null,
        valueProvider: () -> JsonElement,
    ) {
        val statement = if (timestamp == null) {
            """
                select version_history(:table, :operation, :document_id::uuid, :value::jsonb, :user_id)
            """.trimIndent()
        } else {
            """
                select version_history(:table, :operation, :document_id::uuid, :value::jsonb, :user_id, :timestamp)
            """.trimIndent()
        }

        val params = mapOf(
            "operation" to operation,
            "table" to documentClass.table,
            "document_id" to documentId,
            "value" to valueProvider.invoke().toString(),
            "user_id" to userId,
            "timestamp" to timestamp,
        )

        tx.run(queryOf(statement, params).asExecute)
    }
}

enum class DocumentClass(val table: String) {
    AVTALE("avtale_endringshistorikk"),
    TILTAKSGJENNOMFORING("tiltaksgjennomforing_endringshistorikk"),
}
