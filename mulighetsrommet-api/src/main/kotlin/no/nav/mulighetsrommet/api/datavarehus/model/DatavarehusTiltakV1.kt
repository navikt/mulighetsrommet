package no.nav.mulighetsrommet.api.datavarehus.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.AmoKategorisering
import no.nav.mulighetsrommet.model.GjennomforingOppstartstype
import no.nav.mulighetsrommet.model.GjennomforingPameldingType
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Serializable
sealed class DatavarehusTiltakV1 {
    abstract val tiltakskode: Tiltakskode
    abstract val avtale: Avtale?
    abstract val gjennomforing: Gjennomforing

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
        @Serializable(with = LocalDateTimeSerializer::class)
        val opprettetTidspunkt: LocalDateTime,
        @Serializable(with = LocalDateTimeSerializer::class)
        val oppdatertTidspunkt: LocalDateTime,
        val arrangor: Arrangor,
        val navn: String?,
        val oppstartstype: GjennomforingOppstartstype,
        val pameldingstype: GjennomforingPameldingType,
        @Serializable(with = LocalDateSerializer::class)
        val startDato: LocalDate?,
        @Serializable(with = LocalDateSerializer::class)
        val sluttDato: LocalDate?,
        val status: GjennomforingStatusType?,
        val arena: ArenaData?,
        val deltidsprosent: Double?,
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
data class DatavarehusTiltakV1Dto(
    override val tiltakskode: Tiltakskode,
    override val avtale: Avtale?,
    override val gjennomforing: Gjennomforing,
) : DatavarehusTiltakV1()

@Serializable
data class DatavarehusTiltakV1AmoDto(
    override val tiltakskode: Tiltakskode,
    override val avtale: Avtale?,
    override val gjennomforing: Gjennomforing,
    val amoKategorisering: AmoKategorisering?,
) : DatavarehusTiltakV1()

@Serializable
data class DatavarehusTiltakV1YrkesfagDto(
    override val tiltakskode: Tiltakskode,
    override val avtale: Avtale?,
    override val gjennomforing: Gjennomforing,
    val utdanningslop: Utdanningslop?,
) : DatavarehusTiltakV1() {
    @Serializable
    data class Utdanningslop(
        @Serializable(with = UUIDSerializer::class)
        val utdanningsprogram: UUID,
        val utdanninger: Set<
            @Serializable(with = UUIDSerializer::class)
            UUID,
            >,
    )
}
