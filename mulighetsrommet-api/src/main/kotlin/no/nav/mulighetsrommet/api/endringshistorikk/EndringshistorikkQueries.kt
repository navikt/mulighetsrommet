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
            select
                document_id,
                operation,
                edited_at,
                user_id as user_id,
                case
                    when na.nav_ident is not null then concat(na.fornavn, ' ', na.etternavn)
                    else null
                end as user_name
            from endringshistorikk
                left join nav_ansatt na on user_id = na.nav_ident
            where
                document_id = :document_id
                and document_class = '$documentClass'::document_class
            order by edited_at desc;
        """.trimIndent()

        val params = mapOf("document_id" to id)

        val entries = session.list(queryOf(statement, params)) {
            EndringshistorikkDto.Entry(
                id = it.uuid("document_id"),
                operation = it.string("operation"),
                editedAt = it.localDateTime("edited_at"),
                editedBy = EndringshistorikkDto.User.fromAgent(
                    agent = it.string("user_id").toAgent(),
                    navn = it.stringOrNull("user_name"),
                ),
            )
        }

        return EndringshistorikkDto(entries = entries)
    }

    fun logEndring(
        documentClass: DocumentClass,
        operation: String,
        agent: Agent,
        documentId: UUID,
        timestamp: LocalDateTime,
        valueProvider: () -> JsonElement,
    ) {
        @Language("PostgreSQL")
        val query = """
            insert into endringshistorikk (
                document_id,
                document_class,
                value,
                operation,
                edited_at,
                user_id
            ) values (
                :document_id::uuid,
                :document_class::document_class,
                :value::jsonb,
                :operation,
                :edited_at,
                :user_id
            )
        """.trimIndent()

        val params = mapOf(
            "operation" to operation,
            "document_id" to documentId,
            "document_class" to documentClass.name,
            "value" to valueProvider.invoke().toString(),
            "user_id" to agent.textRepr(),
            "edited_at" to timestamp,
        )

        session.execute(queryOf(query, params))
    }
}
