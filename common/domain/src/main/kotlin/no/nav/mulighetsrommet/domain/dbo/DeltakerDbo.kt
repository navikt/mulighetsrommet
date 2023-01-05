package no.nav.mulighetsrommet.domain.dbo

import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.dto.Deltakerstatus
import no.nav.mulighetsrommet.domain.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.time.LocalDateTime
import java.util.*

@Serializable
data class DeltakerDbo(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = UUIDSerializer::class)
    val tiltaksgjennomforingId: UUID,
    val norskIdent: String,
    val status: Deltakerstatus,
    @Serializable(with = LocalDateTimeSerializer::class)
    val fraDato: LocalDateTime? = null,
    @Serializable(with = LocalDateTimeSerializer::class)
    val tilDato: LocalDateTime? = null
)

@Serializable
@Polymorphic
sealed class HistorikkDbo {
    @Serializable(with = UUIDSerializer::class)
    abstract val id: UUID
    abstract val norskIdent: String
    abstract val status: Deltakerstatus

    @Serializable(with = LocalDateTimeSerializer::class)
    abstract val fraDato: LocalDateTime?

    @Serializable(with = LocalDateTimeSerializer::class)
    abstract val tilDato: LocalDateTime?

    @Serializable
    @SerialName("Gruppetiltak")
    class Gruppetiltak(
        @Serializable(with = UUIDSerializer::class)
        override val id: UUID,
        override val norskIdent: String,
        override val status: Deltakerstatus,
        @Serializable(with = LocalDateTimeSerializer::class)
        override val fraDato: LocalDateTime? = null,
        @Serializable(with = LocalDateTimeSerializer::class)
        override val tilDato: LocalDateTime? = null,
        @Serializable(with = UUIDSerializer::class)
        val tiltaksgjennomforingId: UUID
    ) : HistorikkDbo()

    @Serializable
    class IndividueltTiltak(
        @Serializable(with = UUIDSerializer::class)
        override val id: UUID,
        override val norskIdent: String,
        override val status: Deltakerstatus,
        @Serializable(with = LocalDateTimeSerializer::class)
        override val fraDato: LocalDateTime? = null,
        @Serializable(with = LocalDateTimeSerializer::class)
        override val tilDato: LocalDateTime? = null,
        val beskrivelse: String,
        @Serializable(with = UUIDSerializer::class)
        val tiltakstypeId: UUID,
        val virksomhetsnummer: String
    ) : HistorikkDbo()
}
