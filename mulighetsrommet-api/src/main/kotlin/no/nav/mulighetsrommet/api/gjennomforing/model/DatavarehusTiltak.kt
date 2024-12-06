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

sealed class DatavarehusTiltak {
    abstract val tiltakstype: Tiltakstype
    abstract val avtale: Avtale?
    abstract val gjennomforing: Gjennomforing
    abstract val arrangor: Arrangor
    abstract val arena: ArenaData?

    @Serializable
    data class Tiltakstype(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val navn: String,
        val tiltakskode: Tiltakskode,
    )

    @Serializable
    data class Avtale(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val navn: String,
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
    override val tiltakstype: Tiltakstype,
    override val avtale: Avtale?,
    override val gjennomforing: Gjennomforing,
    override val arrangor: Arrangor,
    override val arena: ArenaData?,
) : DatavarehusTiltak()

@Serializable
data class DatavarehusTiltakAmoDto(
    override val tiltakstype: Tiltakstype,
    override val avtale: Avtale?,
    override val gjennomforing: Gjennomforing,
    override val arrangor: Arrangor,
    override val arena: ArenaData?,
    val amoKategorisering: AmoKategorisering?,
) : DatavarehusTiltak()

@Serializable
data class DatavarehusTiltakYrkesfagDto(
    override val tiltakstype: Tiltakstype,
    override val avtale: Avtale?,
    override val gjennomforing: Gjennomforing,
    override val arrangor: Arrangor,
    override val arena: ArenaData?,
    val utdanningslop: Utdanningslop?,
) : DatavarehusTiltak() {
    @Serializable
    data class Utdanningslop(
        val utdanningsprogram: Utdanningsprogram,
        val utdanninger: Set<Utdanning>,
    ) {
        @Serializable
        data class Utdanningsprogram(
            val navn: String,
            val nusKoder: List<String>,
        )

        @Serializable
        data class Utdanning(
            val navn: String,
            val nusKoder: List<String>,
            val sluttkompetanse: Sluttkompetanse,
        )
    }
}
