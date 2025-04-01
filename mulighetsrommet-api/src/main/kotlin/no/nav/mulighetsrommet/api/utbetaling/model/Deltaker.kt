package no.nav.mulighetsrommet.api.utbetaling.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.DeltakerStatus
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Serializable
data class Deltaker(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = UUIDSerializer::class)
    val gjennomforingId: UUID,
    val norskIdent: NorskIdent?,
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate?,
    @Serializable(with = LocalDateSerializer::class)
    val sluttDato: LocalDate?,
    @Serializable(with = LocalDateTimeSerializer::class)
    val registrertTidspunkt: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val endretTidspunkt: LocalDateTime,
    val deltakelsesprosent: Double?,
    val status: DeltakerStatus,
)

@Serializable
data class Deltakelsesmengde(
    @Serializable(with = LocalDateSerializer::class)
    val gyldigFra: LocalDate,
    val deltakelsesprosent: Double,
)
