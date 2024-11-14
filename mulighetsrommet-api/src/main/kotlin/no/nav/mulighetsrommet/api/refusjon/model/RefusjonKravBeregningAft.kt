package no.nav.mulighetsrommet.api.refusjon.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.*

@Serializable
@SerialName("AFT")
data class RefusjonKravBeregningAft(
    override val input: Input,
    override val output: Output,
) : RefusjonKravBeregning() {

    @Serializable
    data class Input(
        override val periode: RefusjonskravPeriode,
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
    @Serializable(with = LocalDateSerializer::class)
    val start: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val slutt: LocalDate,
    // TODO: egen Stillingsprosent-type?
    val stillingsprosent: Double,
)

@Serializable
data class DeltakelseManedsverk(
    @Serializable(with = UUIDSerializer::class)
    val deltakelseId: UUID,
    val manedsverk: Double,
)
