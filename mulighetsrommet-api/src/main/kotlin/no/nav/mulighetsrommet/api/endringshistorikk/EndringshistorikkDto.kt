package no.nav.mulighetsrommet.api.endringshistorikk

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.*
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDateTime
import java.util.*

@Serializable
data class EndringshistorikkDto(
    val entries: List<Entry>,
) {
    @Serializable
    data class Entry(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val operation: String,
        @Serializable(with = LocalDateTimeSerializer::class)
        val editedAt: LocalDateTime,
        val editedBy: User,
    )

    @Serializable
    sealed class User {
        companion object {
            fun fromAgent(agent: Agent, navn: String?): User = when (agent) {
                Arena -> Systembruker("Arena")
                Arrangor -> Systembruker("Arrangør")
                Tiltaksadministrasjon -> Systembruker("System")
                is NavIdent -> NavAnsatt(navIdent = agent.value, navn = navn)
            }
        }
    }

    @Serializable
    data class Systembruker(
        val navn: String,
    ) : User()

    @Serializable
    data class NavAnsatt(
        val navIdent: String,
        val navn: String?,
    ) : User()
}
