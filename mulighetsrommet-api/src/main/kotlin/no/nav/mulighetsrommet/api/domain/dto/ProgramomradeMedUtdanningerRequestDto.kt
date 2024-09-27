package no.nav.mulighetsrommet.api.domain.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.util.*

@Serializable
data class ProgramomradeMedUtdanningerRequestDto(
    @Serializable(with = UUIDSerializer::class)
    val programomradeId: UUID,
    val utdanningsIder: List<
        @Serializable(with = UUIDSerializer::class)
        UUID,
        >,
)
