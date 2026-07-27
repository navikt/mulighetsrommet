package no.nav.mulighetsrommet.admin.opplaring

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.domain.opplaring.Utdanningslop
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.UUID

@Serializable
data class UtdanningslopDetaljer(
    val utdanningsprogram: Utdanningsprogram,
    val utdanninger: List<Utdanning>,
) {
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

fun UtdanningslopDetaljer.toUtdanningslop(): Utdanningslop = Utdanningslop(
    utdanningsprogram = utdanningsprogram.id,
    utdanninger = utdanninger.map { it.id }.toSet(),
)
