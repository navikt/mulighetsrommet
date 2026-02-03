package no.nav.mulighetsrommet.api.avtale.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.UUID

@Serializable
data class Rammedetaljer(
    @Serializable(with = UUIDSerializer::class)
    val avtaleId: UUID,
    val totalRamme: Long,
    val utbetaltArena: Long,
)
