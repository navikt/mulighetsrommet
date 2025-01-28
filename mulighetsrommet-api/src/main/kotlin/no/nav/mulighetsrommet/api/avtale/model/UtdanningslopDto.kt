package no.nav.mulighetsrommet.api.avtale.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import no.nav.mulighetsrommet.utdanning.db.UtdanningslopDbo
import java.util.*

@Serializable
data class UtdanningslopDto(
    val utdanningsprogram: Utdanningsprogram,
    val utdanninger: List<Utdanning>,
) {
    fun toDbo(): UtdanningslopDbo {
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
    )

    @Serializable
    data class Utdanning(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val navn: String,
    )
}
