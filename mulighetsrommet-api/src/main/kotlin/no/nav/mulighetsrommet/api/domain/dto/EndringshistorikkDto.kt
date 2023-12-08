package no.nav.mulighetsrommet.api.domain.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
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
    sealed class User

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
