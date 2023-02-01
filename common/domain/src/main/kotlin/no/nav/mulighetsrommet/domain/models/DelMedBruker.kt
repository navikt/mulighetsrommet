package no.nav.mulighetsrommet.domain.models

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.serializers.LocalDateTimeSerializer
import java.time.LocalDateTime

@Serializable
data class DelMedBruker(
    val id: String? = null,
    val norskIdent: String,
    val navident: String,
    val sanityId: String,
    val dialogId: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val created_at: LocalDateTime? = null,
    @Serializable(with = LocalDateTimeSerializer::class)
    val updated_at: LocalDateTime? = null,
    val created_by: String? = null,
    val updated_by: String? = null
)
