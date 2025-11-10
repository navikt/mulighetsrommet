package no.nav.mulighetsrommet.api.veilederflate.models

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.DataElement
import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.*

@Serializable
data class Deltakelse(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val eierskap: DeltakelseEierskap,
    val tittel: String,
    val tiltakstype: DeltakelseTiltakstype,
    @Serializable(with = LocalDateSerializer::class)
    val innsoktDato: LocalDate?,
    @Serializable(with = LocalDateSerializer::class)
    val sistEndretDato: LocalDate?,
    val periode: DeltakelsePeriode,
    val tilstand: DeltakelseTilstand,
    val status: DeltakelseStatus,
    val pamelding: DeltakelsePamelding?,
)

@Serializable
data class DeltakelsePamelding(
    @Serializable(with = UUIDSerializer::class)
    val gjennomforingId: UUID,
    val status: DeltakerStatusType,
)

@Serializable
enum class DeltakelseEierskap {
    ARENA,
    TEAM_KOMET,
    TEAM_TILTAK,
}

@Serializable
data class DeltakelseTiltakstype(
    val navn: String,
    val tiltakskode: Tiltakskode?,
)

@Serializable
enum class DeltakelseTilstand {
    UTKAST,
    KLADD,
    AKTIV,
    AVSLUTTET,
}

@Serializable
data class DeltakelseStatus(
    val type: DataElement.Status,
    val aarsak: String?,
)

@Serializable
data class DeltakelsePeriode(
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate?,
    @Serializable(with = LocalDateSerializer::class)
    val sluttDato: LocalDate?,
)
