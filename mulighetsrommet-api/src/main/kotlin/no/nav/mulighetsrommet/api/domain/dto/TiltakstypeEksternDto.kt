package no.nav.mulighetsrommet.api.domain.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.dto.Tiltakstypestatus
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.domain.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Serializable
data class TiltakstypeEksternDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val navn: String,
    val arenaKode: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val registrertIArenaDato: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val sistEndretIArenaDato: LocalDateTime,
    @Serializable(with = LocalDateSerializer::class)
    val fraDato: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val tilDato: LocalDate,
    val rettPaaTiltakspenger: Boolean,
    val status: Tiltakstypestatus,
    val deltakerRegistreringInnhold: DeltakerRegistreringInnholdDto?,
)
