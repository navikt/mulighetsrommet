package no.nav.mulighetsrommet.api.veilederflate.models

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDateTime
import java.util.*

@Serializable
data class DeltMedBrukerDto(
    @Serializable(with = UUIDSerializer::class)
    val tiltakId: UUID,
    val deling: DelingMedBruker,
)

@Serializable
data class TiltakDeltMedBrukerDto(
    val tiltak: TiltakDeltMedBruker,
    val deling: DelingMedBruker,
    val tiltakstype: TiltakstypeDeltMedBruker,
)

@Serializable
data class TiltakDeltMedBruker(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val navn: String,
)

@Serializable
data class DelingMedBruker(
    val dialogId: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val tidspunkt: LocalDateTime,
)

@Serializable
data class TiltakstypeDeltMedBruker(
    val tiltakskode: Tiltakskode?,
    val arenakode: String?,
    val navn: String,
)
