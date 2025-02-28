package no.nav.mulighetsrommet.api.endringshistorikk

import kotlinx.serialization.json.JsonElement
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.model.*
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime
import java.util.*

class EndringshistorikkQueries(private val session: Session) {
    fun getEndringshistorikk(documentClass: DocumentClass, id: UUID): EndringshistorikkDto {
        @Language("PostgreSQL")
        val statement = """
            select document_id,
                   operation,
                   lower(sys_period) as edited_at,
                   user_id           as user_id,
                   case
                       when na.nav_ident is not null then concat(na.fornavn, ' ', na.etternavn)
                       else null
                       end           as user_name
            from endringshistorikk
                left join nav_ansatt na on user_id = na.nav_ident
            where
                document_id = :document_id
                and document_class = '$documentClass'::document_class
            order by sys_period desc;
        """.trimIndent()

        val params = mapOf(
            "document_id" to id,
        )

        val entries = session.list(queryOf(statement, params)) {
            val userId = it.string("user_id")

            val editedBy = it.stringOrNull("user_name")
                ?.let { navn -> EndringshistorikkDto.NavAnsatt(userId, navn) }
                ?: EndringshistorikkDto.Systembruker(userId)

            EndringshistorikkDto.Entry(
                id = it.uuid("document_id"),
                operation = it.string("operation"),
                editedAt = it.localDateTime("edited_at"),
                editedBy = editedBy,
            )
        }

        return EndringshistorikkDto(entries = entries)
    }

    fun logEndring(
        documentClass: DocumentClass,
        operation: String,
        agent: Agent,
        documentId: UUID,
        timestamp: LocalDateTime? = null,
        valueProvider: () -> JsonElement,
    ) {
        val statement = if (timestamp == null) {
            """
                select version_history(:operation, :document_id::uuid, :document_class::document_class, :value::jsonb, :user_id)
            """.trimIndent()
        } else {
            """
                select version_history(:operation, :document_id::uuid, :document_class::document_class, :value::jsonb, :user_id, :timestamp)
            """.trimIndent()
        }

        val params = mapOf(
            "operation" to operation,
            "document_id" to documentId,
            "document_class" to documentClass.name,
            "value" to valueProvider.invoke().toString(),
            "user_id" to agent.toUserId(),
            "timestamp" to timestamp,
        )

        session.execute(queryOf(statement, params))
    }
}

fun Agent.toUserId() = when (this) {
    Arena -> "Arena"
    Arrangor -> "Arrangør"
    is NavIdent -> this.value
    Tiltaksadministrasjon -> "System"
}
