package no.nav.mulighetsrommet.domain.models

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.dto.Deltakerstatus
import no.nav.mulighetsrommet.domain.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.time.LocalDateTime
import java.util.*

@Serializable
data class HistorikkForDeltakerDTO(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = LocalDateTimeSerializer::class)
    val fraDato: LocalDateTime? = null,
    @Serializable(with = LocalDateTimeSerializer::class)
    val tilDato: LocalDateTime? = null,
    val status: Deltakerstatus,
    val tiltaksnavn: String?,
    val tiltakstype: String,
    val arrangor: String?
)

data class HistorikkForDeltaker(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = LocalDateTimeSerializer::class)
    val fraDato: LocalDateTime? = null,
    @Serializable(with = LocalDateTimeSerializer::class)
    val tilDato: LocalDateTime? = null,
    val status: Deltakerstatus,
    val tiltaksnavn: String?,
    val tiltakstype: String,
    val virksomhetsnummer: String?
)
