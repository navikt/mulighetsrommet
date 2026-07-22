package no.nav.mulighetsrommet.api.domain.deltaker

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import java.time.LocalDate
import java.time.LocalDateTime

@Serializable
data class Deltakelsesmengde(
    @Serializable(with = LocalDateSerializer::class)
    val gyldigFra: LocalDate,
    val deltakelsesprosent: Double,
    @Serializable(with = LocalDateTimeSerializer::class)
    val opprettetTidspunkt: LocalDateTime = gyldigFra.atStartOfDay(),
)
