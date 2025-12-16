package no.nav.mulighetsrommet.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class TiltakstypeV3Dto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val navn: String,
    val tiltakskode: Tiltakskode,
    val innsatsgrupper: Set<Innsatsgruppe>,
    val arenaKode: String?,
    val deltakerRegistreringInnhold: DeltakerRegistreringInnholdDto?,
    @Serializable(with = LocalDateTimeSerializer::class)
    val opprettetTidspunkt: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val oppdatertTidspunkt: LocalDateTime,
)
