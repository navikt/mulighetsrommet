package no.nav.mulighetsrommet.api.okonomi.models

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.time.LocalDateTime
import java.util.*

@Serializable
data class RefusjonskravDeltakelse(
    @Serializable(with = UUIDSerializer::class)
    val deltakelseId: UUID,
    val perioder: List<DeltakelsePeriode>,
)

@Serializable
data class DeltakelsePeriode(
    @Serializable(with = LocalDateTimeSerializer::class)
    val start: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val slutt: LocalDateTime,
    // TODO: egen Stillingsprosent-type?
    val stillingsprosent: Double,
)
