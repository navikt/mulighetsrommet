package no.nav.mulighetsrommet.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.serializers.InstantSerializer
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.Instant
import java.time.LocalDate
import java.util.*

@Serializable
data class TiltaksgjennomforingV2Dto(
    val tiltakstype: Tiltakstype,
    val arrangor: Arrangor,
    val gjennomforing: Gjennomforing,
) {
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
    sealed class Gjennomforing {
        abstract val id: UUID
        abstract val opprettetTidspunkt: Instant
        abstract val oppdatertTidspunkt: Instant
    }

    @Serializable
    @SerialName("GRUPPE")
    data class Gruppe(
        @Serializable(with = UUIDSerializer::class)
        override val id: UUID,
        @Serializable(with = InstantSerializer::class)
        override val opprettetTidspunkt: Instant,
        @Serializable(with = InstantSerializer::class)
        override val oppdatertTidspunkt: Instant,
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
    ) : Gjennomforing()

    @Serializable
    @SerialName("ENKELTPLASS")
    data class Enkeltplass(
        @Serializable(with = UUIDSerializer::class)
        override val id: UUID,
        @Serializable(with = InstantSerializer::class)
        override val opprettetTidspunkt: Instant,
        @Serializable(with = InstantSerializer::class)
        override val oppdatertTidspunkt: Instant,
    ) : Gjennomforing()
}
