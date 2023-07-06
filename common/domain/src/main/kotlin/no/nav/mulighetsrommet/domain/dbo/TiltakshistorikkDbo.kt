package no.nav.mulighetsrommet.domain.dbo

import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.time.LocalDateTime
import java.util.*

@Serializable
@Polymorphic
sealed class TiltakshistorikkDbo {
    @Serializable(with = UUIDSerializer::class)
    abstract val id: UUID
    abstract val norskIdent: String
    abstract val status: Deltakerstatus

    @Serializable(with = LocalDateTimeSerializer::class)
    abstract val fraDato: LocalDateTime?

    @Serializable(with = LocalDateTimeSerializer::class)
    abstract val tilDato: LocalDateTime?

    @Serializable(with = LocalDateTimeSerializer::class)
    abstract val registrertIArenaDato: LocalDateTime

    @Serializable
    data class Gruppetiltak(
        @Serializable(with = UUIDSerializer::class)
        override val id: UUID,
        override val norskIdent: String,
        override val status: Deltakerstatus,
        @Serializable(with = LocalDateTimeSerializer::class)
        override val fraDato: LocalDateTime?,
        @Serializable(with = LocalDateTimeSerializer::class)
        override val tilDato: LocalDateTime?,
        @Serializable(with = LocalDateTimeSerializer::class)
        override val registrertIArenaDato: LocalDateTime,
        @Serializable(with = UUIDSerializer::class)
        val tiltaksgjennomforingId: UUID,
    ) : TiltakshistorikkDbo()

    @Serializable
    data class IndividueltTiltak(
        @Serializable(with = UUIDSerializer::class)
        override val id: UUID,
        override val norskIdent: String,
        override val status: Deltakerstatus,
        @Serializable(with = LocalDateTimeSerializer::class)
        override val fraDato: LocalDateTime?,
        @Serializable(with = LocalDateTimeSerializer::class)
        override val tilDato: LocalDateTime?,
        @Serializable(with = LocalDateTimeSerializer::class)
        override val registrertIArenaDato: LocalDateTime,
        val beskrivelse: String,
        @Serializable(with = UUIDSerializer::class)
        val tiltakstypeId: UUID,
        val arrangorOrganisasjonsnummer: String,
    ) : TiltakshistorikkDbo()
}
