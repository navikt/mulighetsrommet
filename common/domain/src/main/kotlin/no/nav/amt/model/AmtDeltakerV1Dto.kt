package no.nav.amt.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.DeltakerStatus
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Serializable
data class AmtDeltakerV1Dto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = UUIDSerializer::class)
    val gjennomforingId: UUID,
    val personIdent: String,
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate?,
    @Serializable(with = LocalDateSerializer::class)
    val sluttDato: LocalDate?,
    val status: DeltakerStatus,
    @Serializable(with = LocalDateTimeSerializer::class)
    val registrertDato: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val endretDato: LocalDateTime,
    val dagerPerUke: Float?,
    val prosentStilling: Float?,
)
