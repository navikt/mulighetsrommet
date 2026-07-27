package no.nav.mulighetsrommet.api.domain.deltaker

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.serializers.InstantSerializer
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import java.time.Instant
import java.time.LocalDate

@Serializable
data class Deltakelsesmengde(
    @Serializable(with = LocalDateSerializer::class)
    val gyldigFra: LocalDate,
    val deltakelsesprosent: Double,
    @Serializable(with = InstantSerializer::class)
    val opprettetTidspunkt: Instant,
)
