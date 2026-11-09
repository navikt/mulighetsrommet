@file:UseSerializers(
    InstantSerializer::class,
    LocalDateSerializer::class,
    LocalDateTimeSerializer::class,
    UUIDSerializer::class,
)

package no.nav.mulighetsrommet.api.contracts.datavarehus

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.mulighetsrommet.api.domain.gjennomforing.Artskontokode
import no.nav.mulighetsrommet.model.GjennomforingOppstartstype
import no.nav.mulighetsrommet.model.GjennomforingPameldingType
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.serializers.InstantSerializer
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Serializable
sealed class DatavarehusTiltakV1 {
    abstract val tiltakskode: Tiltakskode
    abstract val artskontokode: Artskontokode?
    abstract val avtale: Avtale?
    abstract val gjennomforing: Gjennomforing

    @Serializable
    data class Avtale(
        val id: UUID,
        val navn: String,
        val opprettetTidspunkt: LocalDateTime,
        val oppdatertTidspunkt: LocalDateTime,
    )

    @Serializable
    data class Gjennomforing(
        val id: UUID,
        val opprettetTidspunkt: LocalDateTime,
        val oppdatertTidspunkt: LocalDateTime,
        val arrangor: Arrangor,
        val navn: String?,
        val oppstartstype: GjennomforingOppstartstype,
        val pameldingstype: GjennomforingPameldingType,
        val startDato: LocalDate?,
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
    override val artskontokode: Artskontokode?,
    override val avtale: Avtale?,
    override val gjennomforing: Gjennomforing,
) : DatavarehusTiltakV1()

@Serializable
data class DatavarehusTiltakV1AmoDto(
    override val tiltakskode: Tiltakskode,
    override val artskontokode: Artskontokode?,
    override val avtale: Avtale?,
    override val gjennomforing: Gjennomforing,
    val amoKategorisering: DvhAmoKategorisering?,
) : DatavarehusTiltakV1()

@Serializable
data class DatavarehusTiltakV1YrkesfagDto(
    override val tiltakskode: Tiltakskode,
    override val artskontokode: Artskontokode?,
    override val avtale: Avtale?,
    override val gjennomforing: Gjennomforing,
    val utdanningslop: DvhUtdanningslop?,
) : DatavarehusTiltakV1()
