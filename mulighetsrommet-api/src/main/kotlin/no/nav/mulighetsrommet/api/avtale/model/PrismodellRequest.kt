package no.nav.mulighetsrommet.api.avtale.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.UUID

@Serializable
data class PrismodellRequest(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val type: PrismodellType,
    val prisbetingelser: String?,
    val satser: List<AvtaltSatsRequest>,
)

@Serializable
data class AvtaltSatsRequest(
    @Serializable(with = LocalDateSerializer::class)
    val gjelderFra: LocalDate?,
    val pris: Int?,
    val valuta: ValutaType,
)
