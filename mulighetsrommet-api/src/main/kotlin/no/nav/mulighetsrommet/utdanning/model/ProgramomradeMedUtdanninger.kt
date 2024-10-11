package no.nav.mulighetsrommet.utdanning.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.domain.dto.ProgramomradeMedUtdanningerRequestDto
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.util.*

@Serializable
data class ProgramomradeMedUtdanninger(
    val programomrade: Programomrade,
    val utdanninger: List<Utdanning>,
) {
    fun toRequest(): ProgramomradeMedUtdanningerRequestDto {
        return ProgramomradeMedUtdanningerRequestDto(
            programomradeId = programomrade.id,
            utdanningsIder = utdanninger.map { it.id },
        )
    }

    @Serializable
    data class Programomrade(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val navn: String,
        val nusKoder: List<String>,
    )

    @Serializable
    data class Utdanning(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val navn: String,
        @Serializable(with = UUIDSerializer::class)
        val programlopStart: UUID,
        val nusKoder: List<String>,
    )
}
