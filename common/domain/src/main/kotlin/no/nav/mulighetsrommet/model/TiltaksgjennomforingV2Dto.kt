package no.nav.mulighetsrommet.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import no.nav.mulighetsrommet.serializers.InstantSerializer
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.Instant
import java.time.LocalDate
import java.util.*

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed class TiltaksgjennomforingV2Dto {
    abstract val id: UUID
    abstract val opprettetTidspunkt: Instant
    abstract val oppdatertTidspunkt: Instant
    abstract val tiltakstype: Tiltakstype
    abstract val arrangor: Arrangor

    @Serializable
    data class Tiltakstype(
        val arenakode: String,
        val tiltakskode: Tiltakskode,
    )

    @Serializable
    data class Arrangor(
        val organisasjonsnummer: Organisasjonsnummer,
    )

    @Serializable
    @SerialName("TiltaksgjennomforingV2.Gruppe")
    data class Gruppe(
        @Serializable(with = UUIDSerializer::class)
        override val id: UUID,
        @Serializable(with = InstantSerializer::class)
        override val opprettetTidspunkt: Instant,
        @Serializable(with = InstantSerializer::class)
        override val oppdatertTidspunkt: Instant,
        override val tiltakstype: Tiltakstype,
        override val arrangor: Arrangor,
        val navn: String,
        @Serializable(with = LocalDateSerializer::class)
        val startDato: LocalDate,
        @Serializable(with = LocalDateSerializer::class)
        val sluttDato: LocalDate?,
        val status: GjennomforingStatusType,
        val oppstart: GjennomforingOppstartstype,
        @Serializable(with = LocalDateSerializer::class)
        val tilgjengeligForArrangorFraOgMedDato: LocalDate?,
        val apentForPamelding: Boolean,
        val antallPlasser: Int,
    ) : TiltaksgjennomforingV2Dto()

    @Serializable
    @SerialName("TiltaksgjennomforingV2.Enkeltplass")
    data class Enkeltplass(
        @Serializable(with = UUIDSerializer::class)
        override val id: UUID,
        @Serializable(with = InstantSerializer::class)
        override val opprettetTidspunkt: Instant,
        @Serializable(with = InstantSerializer::class)
        override val oppdatertTidspunkt: Instant,
        override val tiltakstype: Tiltakstype,
        override val arrangor: Arrangor,
    ) : TiltaksgjennomforingV2Dto()
}
