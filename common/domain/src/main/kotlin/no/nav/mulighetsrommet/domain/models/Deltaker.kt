package no.nav.mulighetsrommet.domain.models

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.serializers.DateSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.time.LocalDateTime
import java.util.*

enum class Deltakerstatus {
    IKKE_AKTUELL,
    VENTER,
    DELTAR,
    AVSLUTTET
}

@Serializable
data class Deltaker(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = UUIDSerializer::class)
    val tiltaksgjennomforingId: UUID,
    val fnr: String,
    val status: Deltakerstatus,
    @Serializable(with = DateSerializer::class)
    val fraDato: LocalDateTime,
    @Serializable(with = DateSerializer::class)
    val tilDato: LocalDateTime? = null
)

@Serializable
data class HistorikkForDeltakerDTO(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = DateSerializer::class)
    val fraDato: LocalDateTime? = null,
    @Serializable(with = DateSerializer::class)
    val tilDato: LocalDateTime? = null,
    val status: Deltakerstatus,
    val tiltaksnavn: String,
    val tiltaksnummer: String,
    val tiltakstype: String,
    val arrangor: String?
)

data class HistorikkForDeltaker(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = DateSerializer::class)
    val fraDato: LocalDateTime? = null,
    @Serializable(with = DateSerializer::class)
    val tilDato: LocalDateTime? = null,
    val status: Deltakerstatus,
    val tiltaksnavn: String,
    val tiltaksnummer: String,
    val tiltakstype: String,
    val virksomhetsnr: String
)
