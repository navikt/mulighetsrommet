package no.nav.mulighetsrommet.domain.dto.amt

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.dto.DeltakerStatus
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.domain.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
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
