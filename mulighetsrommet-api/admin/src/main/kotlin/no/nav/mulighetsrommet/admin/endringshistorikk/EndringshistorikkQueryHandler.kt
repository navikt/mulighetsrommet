package no.nav.mulighetsrommet.admin.endringshistorikk

import kotlinx.serialization.json.JsonElement
import no.nav.mulighetsrommet.model.Agent
import java.time.LocalDateTime
import java.util.UUID

interface EndringshistorikkQueryHandler {
    fun getEndringshistorikk(type: EndringshistorikkType, id: UUID): EndringshistorikkDto

    fun logEndring(
        type: EndringshistorikkType,
        operation: String,
        agent: Agent,
        documentId: UUID,
        timestamp: LocalDateTime,
        valueProvider: () -> JsonElement,
    )
}
