package no.nav.mulighetsrommet.api.domain.dbo

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.time.LocalDateTime
import java.util.*

@Serializable
data class DelMedBrukerDbo(
    val id: String? = null,
    val norskIdent: String,
    val navident: String,
    @Serializable(with = UUIDSerializer::class)
    val sanityId: UUID? = null,
    @Serializable(with = UUIDSerializer::class)
    val tiltaksgjennomforingId: UUID? = null,
    val dialogId: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime? = null,
    @Serializable(with = LocalDateTimeSerializer::class)
    val updatedAt: LocalDateTime? = null,
    val createdBy: String? = null,
    val updatedBy: String? = null,
)
