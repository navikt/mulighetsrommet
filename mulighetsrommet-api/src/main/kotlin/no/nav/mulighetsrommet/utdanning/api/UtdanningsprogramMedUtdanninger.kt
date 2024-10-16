package no.nav.mulighetsrommet.utdanning.api

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.domain.dbo.UtdanningslopDbo
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.util.*

@Serializable
data class UtdanningsprogramMedUtdanninger(
    val utdanningsprogram: Utdanningsprogram,
    val utdanninger: List<Utdanning>,
) {
    fun toRequest(): UtdanningslopDbo {
        return UtdanningslopDbo(
            utdanningsprogram = utdanningsprogram.id,
            utdanninger = utdanninger.map { it.id },
        )
    }

    @Serializable
    data class Utdanningsprogram(
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
