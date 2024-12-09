package no.nav.mulighetsrommet.api.gjennomforing.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.dto.AmoKategorisering
import no.nav.mulighetsrommet.domain.dto.Organisasjonsnummer
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingStatus
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.domain.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import no.nav.mulighetsrommet.utdanning.model.Utdanning.Sluttkompetanse
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Serializable
sealed class DatavarehusTiltak {
    abstract val tiltakskode: Tiltakskode
    abstract val avtale: Avtale?
    abstract val gjennomforing: Gjennomforing
    abstract val arrangor: Arrangor

    @Serializable
    data class Avtale(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val navn: String,
        @Serializable(with = LocalDateTimeSerializer::class)
        val opprettetTidspunkt: LocalDateTime,
        @Serializable(with = LocalDateTimeSerializer::class)
        val oppdatertTidspunkt: LocalDateTime,
    )

    @Serializable
    data class Gjennomforing(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val navn: String,
        @Serializable(with = LocalDateSerializer::class)
        val startDato: LocalDate,
        @Serializable(with = LocalDateSerializer::class)
        val sluttDato: LocalDate?,
        @Serializable(with = LocalDateTimeSerializer::class)
        val opprettetTidspunkt: LocalDateTime,
        @Serializable(with = LocalDateTimeSerializer::class)
        val oppdatertTidspunkt: LocalDateTime,
        val status: TiltaksgjennomforingStatus,
        val arena: ArenaData?,
    )

    @Serializable
    data class Arrangor(
        val organisasjonsnummer: Organisasjonsnummer,
    )

    @Serializable
    data class ArenaData(
        val aar: Int,
        val lopenummer: Int,
    )
}

@Serializable
data class DatavarehusTiltakDto(
    override val tiltakskode: Tiltakskode,
    override val avtale: Avtale?,
    override val gjennomforing: Gjennomforing,
    override val arrangor: Arrangor,
) : DatavarehusTiltak()

@Serializable
data class DatavarehusTiltakAmoDto(
    override val tiltakskode: Tiltakskode,
    override val avtale: Avtale?,
    override val gjennomforing: Gjennomforing,
    override val arrangor: Arrangor,
    val amoKategorisering: AmoKategorisering?,
) : DatavarehusTiltak()

@Serializable
data class DatavarehusTiltakYrkesfagDto(
    override val tiltakskode: Tiltakskode,
    override val avtale: Avtale?,
    override val gjennomforing: Gjennomforing,
    override val arrangor: Arrangor,
    val utdanningslop: Utdanningslop?,
) : DatavarehusTiltak() {
    @Serializable
    data class Utdanningslop(
        val utdanningsprogram: Utdanningsprogram,
        val utdanninger: Set<Utdanning>,
    ) {
        @Serializable
        data class Utdanningsprogram(
            @Serializable(with = UUIDSerializer::class)
            val id: UUID,
            val navn: String,
            @Serializable(with = LocalDateTimeSerializer::class)
            val opprettetTidspunkt: LocalDateTime,
            @Serializable(with = LocalDateTimeSerializer::class)
            val oppdatertTidspunkt: LocalDateTime,
            val nusKoder: List<String>,
        )

        @Serializable
        data class Utdanning(
            @Serializable(with = UUIDSerializer::class)
            val id: UUID,
            val navn: String,
            val sluttkompetanse: Sluttkompetanse,
            @Serializable(with = LocalDateTimeSerializer::class)
            val opprettetTidspunkt: LocalDateTime,
            @Serializable(with = LocalDateTimeSerializer::class)
            val oppdatertTidspunkt: LocalDateTime,
            val nusKoder: List<String>,
        )
    }
}
