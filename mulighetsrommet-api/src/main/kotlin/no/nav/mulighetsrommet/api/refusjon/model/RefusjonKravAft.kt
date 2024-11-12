package no.nav.mulighetsrommet.api.refusjon.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.domain.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Serializable
data class RefusjonKravAft(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val status: RefusjonskravStatus,
    @Serializable(with = LocalDateTimeSerializer::class)
    val fristForGodkjenning: LocalDateTime,
    val tiltakstype: RefusjonskravDto.Tiltakstype,
    val gjennomforing: RefusjonskravDto.Gjennomforing,
    val arrangor: RefusjonskravDto.Arrangor,
    val deltakelser: List<RefusjonKravDeltakelse>,
    val beregning: Beregning,
    val betalingsinformasjon: RefusjonskravDto.Betalingsinformasjon,
) {
    @Serializable
    data class Beregning(
        @Serializable(with = LocalDateTimeSerializer::class)
        val periodeStart: LocalDateTime,
        @Serializable(with = LocalDateTimeSerializer::class)
        val periodeSlutt: LocalDateTime,
        val antallManedsverk: Double,
        val belop: Int,
    )
}

@Serializable
data class RefusjonKravDeltakelse(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate?,
    @Serializable(with = LocalDateSerializer::class)
    val sluttDato: LocalDate?,
    val perioder: List<DeltakelsePeriode>,
    val manedsverk: Double,
    val person: Person?,
    val veileder: String?,
) {
    @Serializable
    data class Person(
        val navn: String,
        @Serializable(with = LocalDateSerializer::class)
        val fodselsdato: LocalDate?,
        val fodselsaar: Int?,
    )
}
