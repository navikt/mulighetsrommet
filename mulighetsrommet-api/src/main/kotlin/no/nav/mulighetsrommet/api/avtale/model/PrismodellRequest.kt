package no.nav.mulighetsrommet.api.avtale.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import java.time.LocalDate

@Serializable
data class PrismodellRequest(
    val type: Prismodell,
    val prisbetingelser: String?,
    val satser: List<AvtaltSatsRequest>,
)

@Serializable
data class AvtaltSatsRequest(
    @Serializable(with = LocalDateSerializer::class)
    val gjelderFra: LocalDate?,
    val pris: Int?,
    val valuta: String?,
    @Serializable(with = LocalDateSerializer::class)
    val gjelderTil: LocalDate? = null,
)
