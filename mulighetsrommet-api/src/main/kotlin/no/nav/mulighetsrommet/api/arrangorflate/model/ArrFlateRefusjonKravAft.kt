package no.nav.mulighetsrommet.api.arrangorflate.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.refusjon.model.RefusjonskravDto
import no.nav.mulighetsrommet.api.refusjon.model.RefusjonskravStatus
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Serializable
data class ArrFlateRefusjonKravAft(
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
        @Serializable(with = LocalDateSerializer::class)
        val periodeStart: LocalDate,
        @Serializable(with = LocalDateSerializer::class)
        val periodeSlutt: LocalDate,
        val antallManedsverk: Double,
        val belop: Int,
        val digest: String,
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
    @Serializable(with = LocalDateSerializer::class)
    val forstePeriodeStartDato: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val sistePeriodeSluttDato: LocalDate,
    val sistePeriodeDeltakelsesprosent: Double,
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
