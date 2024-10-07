package no.nav.mulighetsrommet.api.okonomi.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.time.LocalDateTime
import java.util.*

@Serializable
@SerialName("AFT")
data class RefusjonKravBeregningAft(
    override val input: Input,
    override val output: Output,
) : RefusjonKravBeregning() {

    @Serializable
    data class Input(
        @Serializable(with = LocalDateTimeSerializer::class)
        val periodeStart: LocalDateTime,
        @Serializable(with = LocalDateTimeSerializer::class)
        val periodeSlutt: LocalDateTime,
        val sats: Int,
        val deltakelser: Set<DeltakelsePerioder>,
    ) : RefusjonKravBeregningInput()

    @Serializable
    data class Output(
        override val belop: Int,
        val deltakelser: Set<DeltakelseManedsverk>,
    ) : RefusjonKravBeregningOutput()
}

@Serializable
data class DeltakelsePerioder(
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

@Serializable
data class DeltakelseManedsverk(
    @Serializable(with = UUIDSerializer::class)
    val deltakelseId: UUID,
    val manedsverk: Double,
)
