package no.nav.mulighetsrommet.api.domain.dbo

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.serializers.LocalDateTimeSerializer
import java.time.LocalDateTime

@Serializable
data class DelMedBrukerDbo(
    val id: String? = null,
    val norskIdent: String,
    val navident: String,
    val sanityId: String,
    val dialogId: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime? = null,
    @Serializable(with = LocalDateTimeSerializer::class)
    val updatedAt: LocalDateTime? = null,
    val createdBy: String? = null,
    val updatedBy: String? = null,
)
